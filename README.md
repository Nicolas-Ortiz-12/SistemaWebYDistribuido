# Sistema de Inventario Ferretería

## Secrets de GitHub

`DB_URL`, `DB_USER`, `DB_PASS`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, `JWT_ISSUER`, `JWT_EXPIRATION_MINUTES`, `JWT_REFRESH_EXPIRATION_DAYS`, `APP_CORS_ALLOWED_ORIGINS`.

## Deploy en la VPS

1. Instala Docker Engine, Docker Compose plugin, Java 21 y Node 22 en la VPS o en el self-hosted runner.
2. Clona el repositorio en la VPS y verifica que el runner tenga acceso al socket de Docker.
3. Ajusta `infra/nginx/ferreteria.conf` con la subred privada real del personal.
4. Carga los secretos en GitHub con los nombres indicados arriba.
5. Haz push a `main`.
6. El workflow compilará el frontend, empaquetará los JAR, construirá las imágenes y ejecutará `docker compose up -d --build --remove-orphans`.
7. Comprueba que los servicios queden publicados solo en `127.0.0.1`:
   - frontend `4173`
   - gateway `8080`
   - eureka `8761`
8. Instala Nginx, copia `infra/nginx/ferreteria.conf` a `sites-available`, activa el sitio y recarga el servicio.
9. Valida desde la VPN:

```bash
sudo nginx -t
sudo systemctl reload nginx
curl -I http://tu-dominio-o-ip-interna/
```

## Operación manual

```bash
docker compose --env-file .env.docker up -d --build --remove-orphans
docker image prune -f
docker builder prune -f
```

```bash
docker compose down
```

## Prueba local

1. Copia el ejemplo de entorno:

```bash
cp .env.docker.example .env.docker
```

2. Ajusta los valores en `.env.docker` si vas a usar otra base de datos.
3. Construye el frontend y levanta todo con Docker:

```bash
npm.cmd ci
$env:VITE_API_BASE="/api"; npm.cmd run build
docker compose --env-file .env.docker up -d --build --remove-orphans
```

4. Abre:
   - `http://localhost:4173`
   - `http://localhost:8080`
   - `http://localhost:8761`
