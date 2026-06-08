Write-Host "Compilando Frontend..." -ForegroundColor Cyan
npm run build

Write-Host "Compilando Microservicios..." -ForegroundColor Cyan
$servicios = @("api-gateway", "eureka-server", "Proyecto-Venta", "ProyectoDistri", "Seguridad")

foreach ($servicio in $servicios) {
    Write-Host "-> Compilando $servicio" -ForegroundColor Yellow
    Push-Location $servicio
    mvn clean package -DskipTests
    Pop-Location
}

Write-Host "Reconstruyendo y reiniciando contenedores Docker..." -ForegroundColor Cyan
docker compose --env-file .env up -d --build

Write-Host "¡Todo listo! Los cambios ya estan aplicados." -ForegroundColor Green
