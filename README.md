# Barbería UNEV 2.0

Aplicación JavaFX de escritorio con persistencia JDBC, perfiles de acceso, autenticación TOTP y panel de indicadores. Copia independiente del proyecto académico anterior; no sustituye sus archivos ni su base de datos.

## Requisitos y ejecución

JDK 21 o superior, Maven y servidor MariaDB 10.6+ o MySQL 8.0.16+. JavaFX 21 y sus dependencias se resuelven con Maven. Esta distribución se verificó en Linux x86_64 con JDK 25; para Windows/macOS hay que reconstruir con sus bibliotecas nativas.

1. Importar `database/01_schema.sql` en la base seleccionada. No requiere CREATE DATABASE ni privilegios de triggers en hosting compartido.
2. Importar `database/02_demo.sql` una sola vez en una base vacía para crear las cuentas y datos académicos. Las claves de demostración están en el PDF de entrega, fuera del repositorio; no son claves del servidor.
3. Copiar `config.example.properties` a `config.properties`, completar servidor y credenciales propias y generar una clave de cifrado privada de 32 bytes en Base64. No publicar este archivo. Todas las instalaciones que compartan secretos 2FA deben usar la misma clave de cifrado, distribuida por un canal privado.
4. Ejecutar `mvn javafx:run`, o construir `mvn package` y usar `./scripts/ejecutar.sh`. La aplicación crea `logs/` localmente.

Para preparar automáticamente una instalación local nueva en Linux con MariaDB instalado, ejecute `python3 scripts/preparar-local.py` antes de crear manualmente config.properties. Crea una base aislada, un usuario propio y los datos de demostración sin tocar el servidor del sistema.

En la copia del equipo actual ya existe una configuración local privada y una base de desarrollo aislada. Para iniciarla tras reiniciar, ejecutar `./scripts/iniciar-db-local.sh` y después `./scripts/ejecutar.sh`. Usa el puerto 3308 y una carpeta ignorada `.local-db/`. No cambia el MariaDB del sistema.

## Inicio de sesión

El primer factor se verifica contra BCrypt en la BD. Cada cuenta configura su propio Authenticator en el primer ingreso: escanear el QR (o introducir la clave manualmente), escribir el código actual y confirmar. Los siguientes ingresos piden un código sin volver a mostrar el secreto. Tras tres fallos de 2FA la cuenta se bloquea cinco minutos. Los códigos tienen pasos de 30 segundos y no se pueden reutilizar; hay bitácora de accesos. El administrador puede restablecer el 2FA de una cuenta.

Los secretos TOTP se almacenan cifrados con AES-GCM y la clave externa; no hay códigos ni secretos fijos de autenticación en el programa. Si se pierde la clave de instalación no se pueden descifrar los secretos existentes. No sustituirla sin planificar el restablecimiento de 2FA.

## Funciones

- Administrador: clientes, servicios, citas, cobros, usuarios, roles, activación de cuentas, restablecimiento 2FA y bitácora.
- Recepcionista: clientes, consulta de servicios, gestión de citas y cobros. No administra usuarios ni cambia el catálogo.
- Barbero: consulta sus citas, completa sus servicios y ve sus indicadores/reportes; no accede al directorio de clientes ni a importes financieros.
- Crear/editar clientes, servicios, citas y usuarios mediante modal FXML, con validación y refresco de tablas.
- Confirmación de eliminación; la BD impide borrar clientes o servicios con registros relacionados.
- Agenda valida jornada, fechas futuras y cruces; cobro y comprobante se guardan en una transacción.
- Dashboard: cuatro KPI, gráfico de barras, gráfico de líneas y actividad reciente, calculados con SQL en segundo plano.
- Reportes Excel con usuario, fecha, rango y columnas/filas según el rol. Se usa Apache POI.
- Interfaz adaptable, menú desplegable al reducir a teléfono, tarjetas en 4/2/1 columnas y tablas simplificadas.

Las cifras iniciales son datos de práctica almacenados en la base. No son constantes de los gráficos. Los cambios persistidos se conservan al reiniciar.

## Arquitectura

`db/Database` → `dao/Store` → `service/*` → `controlador/*` → FXML. Las consultas usan PreparedStatement y try-with-resources. `Store.tx` agrupa operaciones atómicas. Los controladores no contienen SQL; las comprobaciones de permisos están también en la capa de servicios. HikariCP mantiene un pool pequeño para no agotar el alojamiento remoto. La sesión expira a los 30 minutos.

`model/` conserva las once entidades del diseño anterior. La persistencia de esta versión utiliza proyecciones JDBC para las tablas y estadísticas; no se presenta como un ORM. El esquema incorpora roles, segundo factor y bitácora.

## Nube

MariaDB puede ser remoto si el proveedor permite conexiones TCP/TLS. MyASP.NET anuncia MySQL: para ese servicio usar `jdbc:mysql:` y Connector/J, incluido en Maven. Para MariaDB se usa su propio conector. Configurar `sslMode=VERIFY_IDENTITY` en MySQL o `sslMode=verify-full` en MariaDB y el nombre de host que corresponda al certificado. La aplicación rechaza conexiones remotas sin verificación TLS. No desactivar la verificación del certificado como solución a un error.

Estado verificado el 24 de noviembre de 2026: alojamiento MyASP.NET activo y base MySQL 8.0.46 creada. Se importaron las 14 tablas y los datos de demostración mediante phpMyAdmin; se comprobaron 15 claves foráneas, 3 usuarios, 8 clientes, 24 citas y 18 pagos (8100.00). Las cuentas todavía no tienen Authenticator vinculado. La conexión JDBC remota sigue pendiente: el servidor presenta un certificado autofirmado automático sin identidad del nombre del servidor, incompatible con VERIFY_IDENTITY. Solicitar al proveedor un endpoint con certificado válido y su cadena de confianza. No desactivar la verificación TLS. La configuración privada de nube está separada de la configuración local y requiere completar la contraseña localmente. No es una aplicación web y no debe subirse el JAR al hosting.

## Pruebas y distribución

```
mvn test
env BARBERIA_DB_TEST=true mvn test
env BARBERIA_DB_TEST=true BARBERIA_UI=true mvn package
java -cp target/barberia-v2-2.0.0.jar edu.unev.barberia.tools.CheckConnection
```

Las pruebas de BD necesitan las cuentas de práctica. Las pruebas gráficas necesitan sesión de escritorio y las claves de las cuentas de prueba en el archivo privado (`demo.admin.password`, `demo.recepcion.password`, `demo.barbero.password`). Restablecen 2FA de esas tres cuentas DEMO para permitir que el propietario las vincule después; no ejecutarlas sobre cuentas personales. Las pruebas funcionales crean registros temporales y los eliminan, sin modificar el proyecto original.

El JAR de Maven incluye dependencias y punto de entrada `Launcher`; `jpackage` genera una app-image Linux con runtime. Para usarla desde este equipo, iniciar la base de desarrollo y ejecutarla desde la carpeta del proyecto para que encuentre config.properties. La distribución no incluye credenciales, claves privadas ni secretos TOTP. Código publicado en https://github.com/Jeremoide/barberia-unev-tarea4. La versión original está separada en https://github.com/Jeremoide/barberia-unev. Los ejecutables siguen disponibles localmente; todavía no se ha publicado un enlace de descarga del ejecutable.

## Uso de IA

Codex asistió en diagnóstico, diseño del esquema y de permisos, implementación Java/FXML/CSS, seguridad, pruebas y documentación. Se revisaron y corrigieron el diseño móvil, el flujo de verificación, la carga de datos y el empaquetado a partir de compilación, pruebas y capturas. El estudiante debe revisar las modificaciones y comprender las reglas antes de presentar. No se atribuyen modificaciones manuales del código al estudiante sin su confirmación.
