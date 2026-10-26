# SistemaWebYDistribuido
En este repositoria se cargaran los avances del proyecto de Sistema Web Y Distribuido 

## Secretos

Las credenciales **no van en el repo**. Seguridad, Venta, Compra y el gateway leen
`~/.sistemaweb/secrets.properties` (o la ruta de la variable `SISTEMAWEB_SECRETS`):

```properties
DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require&preferQueryMode=simple
DB_USER=<usuario>
DB_PASS=<contraseña>
# Mínimo 32 caracteres; el MISMO valor en todas las PCs del cluster
JWT_SECRET=<generar con: openssl rand -base64 48>
```

Cada PC que corra el Tomcat de microservicios o el gateway necesita este archivo
(el Tomcat y IntelliJ lo encuentran en la carpeta del usuario que los ejecuta).
Las variables de entorno con el mismo nombre tienen prioridad sobre el archivo.
