# =====================================================================
#  Configuración de la arquitectura distribuida.
#  En una sola PC sirve tal cual. Para varias PCs, editar las IPs de los
#  componentes y, en cada PC, levantar solo las instancias que le tocan:
#     .\infra\cluster.ps1 start -Instances ui2,ms     (ejemplo PC2)
# =====================================================================

# IP de ESTA PC en la red local (se detecta sola; se puede fijar a mano)
$LanIp = '172.20.10.2'
# Detección automática (descomentar para volver a usarla):
# $LanIp = (Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias 'Wi-Fi', 'Ethernet' -ErrorAction SilentlyContinue |
#           Where-Object { $_.IPAddress -notlike '169.254.*' } | Select-Object -First 1).IPAddress
# if (-not $LanIp) { $LanIp = '127.0.0.1' }

# Dónde corre cada componente (IP de la PC que lo hospeda)
$Hosts = @{
    ui1     = $LanIp      # Tomcat UI 1
    ui2     = $LanIp      # Tomcat UI 2
    ms      = $LanIp      # Tomcat de microservicios (Seguridad, Venta, Compra)
    gateway = $LanIp      # API Gateway (IntelliJ)
    eureka  = $LanIp      # Eureka Server
    # Redis arranca en "protected mode": sin contraseña solo acepta conexiones
    # locales. Si está en la misma PC que el Tomcat ms se usa 127.0.0.1; si está
    # en otra PC, poner su IP y configurar 'requirepass' en redis.conf + $RedisPassword.
    redis   = '127.0.0.1'
}
$RedisPassword = ''

# Rutas locales
$CatalinaHome = 'C:\tomcat'      # instalación de Tomcat 10.1 (binarios compartidos)
$ClusterRoot  = 'C:\cluster'     # aquí se crea un CATALINA_BASE por instancia
$ApacheRoot   = 'C:\Apache24'    # instalación de Apache httpd (Apache Lounge)
$ApachePort   = 80

# Secreto compartido entre Apache y los conectores AJP de Tomcat
$AjpSecret = 'ajp-cluster-sistemaweb'

# Puertos por instancia (fijos, así cada PC conoce los de las demás)
$InstanceConfig = [ordered]@{
    ms  = @{ jvmRoute = 'ms1'; shutdown = 8005; http = 8083; ajp = 8009; cluster = 4000; heap = '1536m' }
    ui1 = @{ jvmRoute = 'ui1'; shutdown = 8006; http = 8091; ajp = 8010; cluster = 4001; heap = '256m'  }
    ui2 = @{ jvmRoute = 'ui2'; shutdown = 8007; http = 8092; ajp = 8011; cluster = 4002; heap = '256m'  }
}
