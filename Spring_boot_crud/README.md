# Backend de productos: MySQL y MongoDB

Java 21 como objetivo de compilación, Spring Boot 4.1.1 y Maven Wrapper 3.9.9. API en el puerto **8081**.

Consulta el [README principal](../README.md) para instalar las bases, conocer la arquitectura, las variables, ejemplos HTTP y limitaciones.

## Ejecutar

Prepara MySQL ejecutando [dbs/productoscruddb.sql](dbs/productoscruddb.sql) e inicia MongoDB local. Define las credenciales de tu instalación:

```powershell
$env:DB_USER = "root"
$env:DB_PASSWORD = "TU_CLAVE_LOCAL"
$env:MONGO_URI = "mongodb://localhost:27017/productoscrudmongo"
.\mvnw.cmd spring-boot:run
```

`JAVA_HOME` debe apuntar a un JDK 21 o superior. Spring no carga automáticamente archivos `.env`.

## Rutas

- `/productos`: CRUD original, conserva MySQL.
- `/api/mysql/productos`: CRUD SQL explícito.
- `/api/mongodb/productos`: CRUD MongoDB.
- `/api/mongodb/productos/filtros/nombre?texto=mouse`.
- `/api/mongodb/productos/filtros/precios?minimo=0&maximo=100000`.
- `/api/mongodb/productos/filtros/costosos?limite=5`.
- `/api/mongodb/productos/filtros/estadisticas`.

Los CRUD admiten GET/POST en la base y GET/PUT/DELETE en `/{id}`. Los filtros son GET y devuelven resultados más el pipeline ejecutado como Extended JSON. Ambos adaptadores extienden `ProductRepository`; dos servicios calificados reutilizan los mismos casos de uso. Los tipos BSON permanecen dentro del adaptador MongoDB.

## Comprobar

```powershell
.\mvnw.cmd test
.\mvnw.cmd compile
```

Estas pruebas no requieren servidores. Para integración real con MongoDB:

```powershell
$env:MONGO_TEST_URI = "mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=5000"
.\mvnw.cmd -Pintegration verify
```

Si el JAR está bloqueado en Windows, omite el empaquetado:

```powershell
.\mvnw.cmd -Pintegration test-compile failsafe:integration-test failsafe:verify
```

Las integraciones crean y eliminan exclusivamente bases temporales. La integración MySQL real se activa con `MYSQL_TEST_URL` (URL de servidor sin nombre de base), `MYSQL_TEST_USER` y `MYSQL_TEST_PASSWORD`; requiere permisos CREATE/DROP DATABASE. Sin esa URL queda omitida. Las instrucciones completas están en el README principal.

El frontend está en [../react_crud](../react_crud), puerto **5175**. CORS permite por defecto `localhost:5175` y `127.0.0.1:5175`; se ajusta con `CORS_ALLOWED_ORIGINS`.
