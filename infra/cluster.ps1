<#
.SYNOPSIS
  Administra la arquitectura distribuida: Apache balanceador, 2 Tomcats de UI
  y 1 Tomcat de microservicios, todos en cluster.

.EXAMPLE
  .\infra\cluster.ps1 setup            # crea las instancias y configura Apache
  .\infra\cluster.ps1 build            # compila los WAR y el front
  .\infra\cluster.ps1 deploy           # copia WARs y front a los Tomcats
  .\infra\cluster.ps1 start            # levanta todo lo de esta PC
  .\infra\cluster.ps1 status
  .\infra\cluster.ps1 stop
  .\infra\cluster.ps1 start -Instances ui2,ms   # solo algunas (otra PC)
#>
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('setup', 'build', 'deploy', 'start', 'stop', 'status')]
    [string]$Action,

    # ms, ui1, ui2, apache
    [string[]]$Instances = @('ms', 'ui1', 'ui2', 'apache')
)

$ErrorActionPreference = 'Stop'
$Repo = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'cluster.config.ps1')

# Acepta "-Instances ms,ui1" tanto desde PowerShell como desde cmd (-File)
$Instances = @($Instances | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
$valid = @($InstanceConfig.Keys) + 'apache'
foreach ($i in $Instances) {
    if ($valid -notcontains $i) { throw "Instancia desconocida '$i'. Validas: $($valid -join ', ')" }
}
$Tomcats = @($Instances | Where-Object { $_ -ne 'apache' })
$Utf8NoBom = New-Object System.Text.UTF8Encoding $false

function Write-Step($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }

function Get-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) { return $env:JAVA_HOME }
    $line = (& cmd /c 'java -XshowSettings:properties -version 2>&1') | Select-String 'java.home = '
    if (-not $line) { throw 'No se encontro Java 21 (configurar JAVA_HOME)' }
    return ($line.ToString() -split '= ', 2)[1].Trim()
}

function Test-Port($port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

# ---------------------------------------------------------------- setup
function Invoke-Setup {
    $javaHome = Get-JavaHome
    foreach ($name in $Tomcats) {
        $c = $InstanceConfig[$name]
        $base = Join-Path $ClusterRoot $name
        Write-Step "Instancia Tomcat '$name' en $base"
        foreach ($d in 'bin', 'conf', 'logs', 'temp', 'webapps', 'work') {
            New-Item -ItemType Directory -Force (Join-Path $base $d) | Out-Null
        }
        Copy-Item "$CatalinaHome\conf\*" "$base\conf" -Recurse -Force
        Copy-Item "$PSScriptRoot\tomcat\server.xml" "$base\conf\server.xml" -Force

        $javaOpts = @(
            "-Dshutdown.port=$($c.shutdown)", "-Dhttp.port=$($c.http)", "-Dajp.port=$($c.ajp)",
            "-DjvmRoute=$($c.jvmRoute)", "-Dcluster.address=$($Hosts[$name])", "-Dcluster.port=$($c.cluster)",
            "-Dajp.secret=$AjpSecret", '-Djava.net.preferIPv4Stack=true', '-Dfile.encoding=UTF-8'
        )
        $lines = @(
            '@echo off',
            'rem Generado por infra\cluster.ps1 setup - no editar a mano',
            "set `"JAVA_HOME=$javaHome`"",
            "set `"TITLE=Tomcat $name ($($c.jvmRoute))`""
        )
        if ($name -eq 'ms') {
            # Variables que leen los microservicios (application.properties)
            $javaOpts += "-Deureka.instance.hostname=$($Hosts.ms)", "-Deureka.instance.non-secure-port=$($c.http)"
            $lines += "set `"REDIS_HOST=$($Hosts.redis)`""
            if ($RedisPassword) { $lines += "set `"REDIS_PASS=$RedisPassword`"" }
            $lines += "set `"EUREKA_URL=http://$($Hosts.eureka):8761/eureka/`""
        }
        $lines += "set `"JAVA_OPTS=$($javaOpts -join ' ')`""
        $lines += "set `"CATALINA_OPTS=-Xms128m -Xmx$($c.heap)`""
        Set-Content -Path "$base\bin\setenv.bat" -Value $lines -Encoding ASCII
    }

    if ($Instances -contains 'apache') { Invoke-ApacheSetup }
}

function Invoke-ApacheSetup {
    Write-Step "Apache httpd en $ApacheRoot"
    $httpdConf = "$ApacheRoot\conf\httpd.conf"
    if (-not (Test-Path $httpdConf)) { throw "No existe $httpdConf (instalar Apache Lounge en $ApacheRoot)" }

    $conf = [IO.File]::ReadAllText($httpdConf)
    $srv = $ApacheRoot -replace '\\', '/'
    $conf = $conf -replace '(?m)^Define SRVROOT .*$', "Define SRVROOT `"$srv`""
    $conf = $conf -replace '(?m)^Listen \d+\s*$', "Listen $ApachePort"
    $conf = $conf -replace '(?m)^#?ServerName .*$', "ServerName localhost:$ApachePort"
    foreach ($m in 'proxy', 'proxy_ajp', 'proxy_http', 'proxy_balancer', 'proxy_hcheck', 'lbmethod_byrequests',
                   'slotmem_shm', 'watchdog', 'headers', 'status') {
        $conf = $conf -replace "(?m)^#\s*(LoadModule ${m}_module )", '$1'
    }
    if ($conf -notmatch 'conf/extra/httpd-cluster\.conf') {
        $conf = $conf.TrimEnd() + "`r`n`r`n# Balanceador del cluster SistemaWeb`r`nInclude conf/extra/httpd-cluster.conf`r`n"
    }
    [IO.File]::WriteAllText($httpdConf, $conf, $Utf8NoBom)

    $tpl = [IO.File]::ReadAllText("$PSScriptRoot\apache\httpd-cluster.conf")
    $tpl = $tpl.Replace('@UI1_HOST@', $Hosts.ui1).Replace('@UI1_AJP@', [string]$InstanceConfig.ui1.ajp)
    $tpl = $tpl.Replace('@UI2_HOST@', $Hosts.ui2).Replace('@UI2_AJP@', [string]$InstanceConfig.ui2.ajp)
    $tpl = $tpl.Replace('@GATEWAY_HOST@', $Hosts.gateway).Replace('@AJP_SECRET@', $AjpSecret)
    [IO.File]::WriteAllText("$ApacheRoot\conf\extra\httpd-cluster.conf", $tpl, $Utf8NoBom)

    & "$ApacheRoot\bin\httpd.exe" -t
    if ($LASTEXITCODE -ne 0) { throw 'La configuracion de Apache tiene errores' }
}

# ---------------------------------------------------------------- build
function Invoke-Build {
    if ($Tomcats -contains 'ms') {
        foreach ($svc in 'Seguridad', 'Proyecto-Venta', 'ProyectoDistri') {
            Write-Step "Compilando WAR de $svc"
            Push-Location (Join-Path $Repo $svc)
            try {
                & mvn -q -DskipTests clean package
                if ($LASTEXITCODE -ne 0) {
                    # El autobuild del IDE escribe en el mismo target/ y a veces pisa a Maven; se reintenta una vez
                    Write-Warning "Fallo la compilacion de $svc, reintentando..."
                    & mvn -q -DskipTests clean package
                    if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion de $svc" }
                }
            } finally { Pop-Location }
        }
    }
    if ($Tomcats | Where-Object { $_ -like 'ui*' }) {
        Write-Step 'Compilando el front (vite build)'
        Push-Location (Join-Path $Repo 'SistemaWeb-front')
        try {
            if (-not (Test-Path 'node_modules')) { & npm install; if ($LASTEXITCODE -ne 0) { throw 'Fallo npm install' } }
            & npm run build
            if ($LASTEXITCODE -ne 0) { throw 'Fallo el build del front' }
        } finally { Pop-Location }
    }
}

# ---------------------------------------------------------------- deploy
function Invoke-Deploy {
    foreach ($name in $Tomcats) {
        $webapps = Join-Path $ClusterRoot "$name\webapps"
        if (-not (Test-Path $webapps)) { throw "Falta la instancia '$name': ejecutar primero 'setup'" }
        if ($name -eq 'ms') {
            $wars = @{
                'Seguridad.war' = 'Seguridad\target\Seguridad-*.war'
                'Venta.war'     = 'Proyecto-Venta\target\proyectoVenta-*.war'
                'Compra.war'    = 'ProyectoDistri\target\ProyectoDistri-*.war'
            }
            foreach ($dest in $wars.Keys) {
                $src = Get-ChildItem (Join-Path $Repo $wars[$dest]) -ErrorAction SilentlyContinue | Select-Object -First 1
                if (-not $src) { throw "No se encontro $($wars[$dest]): ejecutar 'build'" }
                Write-Step "ms <- $dest"
                Copy-Item $src.FullName (Join-Path $webapps $dest) -Force
            }
        } else {
            $dist = Join-Path $Repo 'SistemaWeb-front\dist'
            if (-not (Test-Path "$dist\index.html")) { throw "No existe ${dist}: ejecutar 'build'" }
            Write-Step "$name <- UI (ROOT)"
            $root = Join-Path $webapps 'ROOT'
            if (Test-Path $root) { Remove-Item $root -Recurse -Force }
            Copy-Item $dist $root -Recurse
        }
    }
}

# ---------------------------------------------------------------- start / stop
function Invoke-Catalina($name, $cmd) {
    $env:CATALINA_HOME = $CatalinaHome
    $env:CATALINA_BASE = Join-Path $ClusterRoot $name
    & "$CatalinaHome\bin\catalina.bat" $cmd
}

function Invoke-Start {
    foreach ($name in $Tomcats) {
        if (Test-Port $InstanceConfig[$name].http) { Write-Host "  $name ya esta corriendo"; continue }
        Write-Step "Iniciando Tomcat $name (puerto $($InstanceConfig[$name].http))"
        Invoke-Catalina $name 'start'
    }
    if ($Instances -contains 'apache') {
        if (Get-Process httpd -ErrorAction SilentlyContinue) { Write-Host '  apache ya esta corriendo' }
        else {
            Write-Step "Iniciando Apache (puerto $ApachePort)"
            Start-Process "$ApacheRoot\bin\httpd.exe" -WorkingDirectory "$ApacheRoot\bin"
        }
    }
}

function Invoke-Stop {
    if ($Instances -contains 'apache') {
        Write-Step 'Deteniendo Apache'
        Get-Process httpd -ErrorAction SilentlyContinue | Stop-Process -Force
    }
    foreach ($name in $Tomcats) {
        if (-not (Test-Port $InstanceConfig[$name].shutdown)) { Write-Host "  $name no esta corriendo"; continue }
        Write-Step "Deteniendo Tomcat $name"
        Invoke-Catalina $name 'stop'
    }
}

# ---------------------------------------------------------------- status
function Get-Http($url) {
    try {
        $r = Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 5 -MaximumRedirection 0
        return [string]$r.StatusCode
    } catch {
        if ($_.Exception.Response) { return [string][int]$_.Exception.Response.StatusCode }
        return 'sin respuesta'
    }
}

function Invoke-Status {
    $rows = @()
    foreach ($name in $Tomcats) {
        $c = $InstanceConfig[$name]
        $h = $Hosts[$name]
        $path = if ($name -eq 'ms') { '/Seguridad/auth/me' } else { '/session.jsp' }
        $rows += [pscustomobject]@{ Componente = "Tomcat $name ($($c.jvmRoute))"; URL = "http://${h}:$($c.http)$path"; HTTP = Get-Http "http://${h}:$($c.http)$path" }
    }
    if ($Instances -contains 'apache') {
        $rows += [pscustomobject]@{ Componente = 'Apache UI'; URL = "http://localhost:$ApachePort/"; HTTP = Get-Http "http://localhost:$ApachePort/" }
        $rows += [pscustomobject]@{ Componente = 'Apache balancer-manager'; URL = "http://localhost:$ApachePort/balancer-manager"; HTTP = Get-Http "http://localhost:$ApachePort/balancer-manager" }
    }
    $rows += [pscustomobject]@{ Componente = 'API Gateway'; URL = "http://$($Hosts.gateway):8080/auth/me"; HTTP = Get-Http "http://$($Hosts.gateway):8080/auth/me" }
    $rows += [pscustomobject]@{ Componente = 'Eureka'; URL = "http://$($Hosts.eureka):8761/"; HTTP = Get-Http "http://$($Hosts.eureka):8761/" }
    $rows | Format-Table -AutoSize
    Write-Host '(401 en /auth/me es correcto: el servicio responde y exige token)'
}

switch ($Action) {
    'setup'  { Invoke-Setup }
    'build'  { Invoke-Build }
    'deploy' { Invoke-Deploy }
    'start'  { Invoke-Start }
    'stop'   { Invoke-Stop }
    'status' { Invoke-Status }
}
