# Informe de implementación

Fecha: 17 de octubre de 2026. Rama: `feature/mongodb-persistence`.

## Resultado

El proyecto conserva el CRUD SQL y añade MongoDB mediante la clase abstracta `ProductRepository`. React selecciona la persistencia real y permite ejecutar cuatro agregaciones con visualización y copia del pipeline utilizado. El modelo `Product`, el contrato CRUD, el adaptador MySQL y el esquema SQL conservan su estructura original.

## Decisiones

- Dos beans de `ProductService`, conectados mediante calificadores explícitos, reutilizan los casos de uso. El controlador selecciona entre ellos por la ruta. `/productos` sigue usando MySQL.
- `ProductAggregationRepository` define únicamente las cuatro capacidades adicionales. `ProductQueryService` valida parámetros sin depender de Spring, JDBC ni MongoDB. No se creó otra fábrica abstracta.
- El adaptador MongoDB transforma documentos BSON en objetos del dominio. Almacena importes `Decimal128`, reserva IDs enteros con un contador atómico y evita sobrescribir IDs importados. Los reintentos están acotados.
- Las etapas BSON ejecutadas se convierten en el arreglo Extended JSON de la respuesta. Los decimales del pipeline se representan con `$numberDecimal`; no se generan ejemplos independientes del código ejecutado.
- El frontend reutiliza formulario y tabla, cancela peticiones al cambiar de tecnología/filtro y reinicia las consultas después de una mutación. Envía el precio como cadena decimal y conserva el contrato de respuesta original.
- Los errores controlados incluyen mensajes legibles y CORS se limita a orígenes locales configurables.

## Archivos creados

Rutas Java relativas a `Spring_boot_crud/src/main/java/com/example/productoscrud/`:

- `application/ports/ProductAggregationRepository.java`.
- `application/query/AggregationResult.java` y `ProductStatistics.java`.
- `application/service/ProductQueryService.java`.
- `infrastructure/persistence/mongodb/ProductRepositoryMongoDB.java`.
- `infrastructure/web/controllers/ProductQueryController.java`.
- `infrastructure/web/ApiExceptionHandler.java` y `WebConfiguration.java`.

Pruebas en `Spring_boot_crud/src/test/java/com/example/productoscrud/`:

- `ProductServiceTest.java` y `ProductQueryServiceTest.java`.
- `ProductRepositoryMySQLTest.java` y `ProductRepositoryMongoDBTest.java`.
- `ProductHttpTest.java` y `AppWiringTest.java`.
- `MongoPersistenceIT.java` y `MySQLPersistenceIT.java`.

Recursos de pruebas: `Spring_boot_crud/src/test/resources/logback-test.xml` y `mockito-extensions/org.mockito.plugins.MockMaker`.

Frontend, relativo a `react_crud/src/`:

- `components/DatabaseSelector.jsx`, `ProductForm.jsx`, `ProductTable.jsx` y `MongoFilters.jsx`.
- `services/api.js`.
- `test/setup.js`, `test/App.test.jsx` y `test/MongoFilters.test.jsx`.

Documentación: este informe, `docs/IMPLEMENTACION.md`.

## Archivos modificados

- `.gitignore`: excluye la caché local de npm.
- `README.md`, `Spring_boot_crud/README.md` y `react_crud/README.md`: instalación, ejecución, arquitectura, rutas, variables, pruebas y limitaciones.
- `Spring_boot_crud/pom.xml`: conserva el starter MongoDB previamente agregado y añade dependencias de pruebas y perfil Failsafe.
- `Spring_boot_crud/mvnw.cmd`: comprueba el atributo `Target` antes de indexarlo para admitir carpetas `.m2` normales en Windows.
- `Spring_boot_crud/src/main/resources/application.properties`: conserva la URI MongoDB y la contraseña vacía existentes; añade CORS configurable.
- `Spring_boot_crud/src/main/java/com/example/productoscrud/app/App.java`: composición explícita de ambos servicios y servicio de consultas.
- `Spring_boot_crud/src/main/java/com/example/productoscrud/application/service/ProductService.java`: validación compartida del dominio e IDs.
- `Spring_boot_crud/src/main/java/com/example/productoscrud/infrastructure/web/controllers/ProductController.java`: rutas seleccionables y alias original.
- `react_crud/package.json`, `package-lock.json` y `vite.config.js`: configuración de pruebas y dependencias de desarrollo.
- `react_crud/src/App.jsx` y `styles.css`: selección funcional, componentes reutilizables, control de peticiones y presentación adaptable.

Los cambios locales que ya existían en `pom.xml` y `application.properties` se analizaron y conservaron. Durante el trabajo apareció `Spring_boot_crud/dbs/productoscrudmongo.db`, un archivo local de ejemplos mongosh con IDs explícitos; no fue creado ni modificado por esta implementación y permanece sin seguimiento de Git.

## Comandos y comprobaciones

Se inspeccionaron los archivos del proyecto, `git status`, `git branch --show-current` y `git diff` antes de modificar código. Se consultó documentación oficial de Spring Boot, Spring Data y MongoDB para la configuración, decimales e instalación en Windows.

Los comandos Maven utilizaron el JDK local 22 mediante `JAVA_HOME` de esa terminal; el POM mantiene `release 21`. No se cambió la configuración global de Java.

| Comando / comprobación | Resultado |
| --- | --- |
| `.\mvnw.cmd test` | 19 pruebas del backend aprobadas en la ejecución final, también ejecutadas dentro del comando combinado siguiente |
| `.\mvnw.cmd -B -ntp -Pintegration test failsafe:integration-test failsafe:verify` | 19 pruebas habituales y 5 integraciones MongoDB aprobadas; 1 integración MySQL real omitida por falta de `MYSQL_TEST_URL` |
| `.\mvnw.cmd compile` | Compilación dirigida a Java 21 |
| `npm ci --cache .npm-cache --no-audit --no-fund` | Instalación reproducible con el lockfile completada |
| `npm test` | 7 pruebas de React aprobadas |
| `npm run build` | Compilación Vite completada; salida en `react_crud/dist/` |
| `git diff --check` | Revisión de espacios de los cambios |

La integración se ejecutó contra el servidor MongoDB local usando bases temporales con UUID. Los casos verificaron CRUD real, actualización sin cambios, representación decimal, búsqueda literal, límites inclusivos, orden descendente, estadísticas vacías y con datos, reejecución del pipeline devuelto, 40 creaciones concurrentes, IDs importados y separación respecto al adaptador SQL ejecutado en H2.

Las pruebas habituales verificaron validaciones, selección de repositorios, arranque Spring sin ambigüedad, HTTP 200/201/204/400/404/503, CORS y la representación exacta de las etapas. El CRUD SQL se ejecutó en H2 con modo MySQL; esto no equivale a una prueba contra el servidor MySQL real.

React se comprobó mediante jsdom: CRUD en ambas rutas, conservación de la entrada decimal, errores recuperables, cambio rápido de base/filtro, validación de rangos y copia exacta del pipeline. No se realizó una inspección visual en un navegador real.

## Problemas encontrados y pendientes

1. El wrapper original fallaba al indexar `Target` cuando no había un enlace simbólico. Se corrigió esa comprobación. El `JAVA_HOME` de la terminal también requirió apuntar explícitamente al JDK instalado.
2. `.\mvnw.cmd -Pintegration verify` superó los tests, pero falló en `spring-boot:repackage` al renombrar el JAR de `target` a `.jar.original`. Es el bloqueo local de Windows previsto en el encargo. Las integraciones se ejecutaron satisfactoriamente invocando Failsafe sin empaquetar. No se ejecutó `clean` ni se finalizaron procesos Java.
3. La integración contra un servidor MySQL real queda disponible pero omitida hasta proporcionar credenciales y `MYSQL_TEST_URL`. Solo crea y elimina su esquema temporal. No se hicieron escrituras ni borrados sobre la base SQL existente.
4. Los ejemplos MongoDB con IDs manuales requieren mantener el contador al importar grandes lotes. La implementación tolera las colisiones iniciales de forma segura y acotada; el README documenta cómo sincronizarlo con `$max`.
5. La publicación en GitHub queda pendiente de autorización explícita. No se hicieron commits, push, merge ni pull requests.

No quedan funcionalidades del CRUD o de los cuatro pipelines pendientes de implementación. La comprobación contra MySQL real, la revisión visual manual y el empaquetado del JAR en este entorno conservan las limitaciones indicadas.
