# Barbería UNEV 3.0

Aplicación de escritorio JavaFX para citas, servicios, clientes, inventario, ventas, caja y fidelización. Autor: Jeremy Peguero Almonte, matrícula 2022-3200298.

Esta versión amplía la 2.0 sin modificar su carpeta ni la Release DevOps anterior. La rama `codex/version-3.0` contiene la nueva versión. Los datos de demostración son ficticios y están persistidos en una base independiente; no representan ventas reales.

## Inicio rápido

Requisitos: Java 21 o superior, Python 3.10 o superior, Docker o Podman iniciado y acceso a Internet la primera vez para descargar MariaDB. En Linux se requiere GTK 3. Los paquetes son para equipos x64. Windows usa `python` y Linux puede usar `python3`.

1. Descargue el ZIP correspondiente a su sistema desde la Release `v3.0.1` y extráigalo.
2. Abra una terminal dentro de la carpeta extraída.
3. Prepare la base: `python scripts/preparar-demo.py`.
4. Compruebe JDBC: `python scripts/abrir-demo.py --check`.
5. Abra la aplicación: `python scripts/abrir-demo.py`.

Desde el código fuente, ejecute primero `mvn -B package -DskipTests` y después los pasos anteriores. El script detecta el JAR de `target/`. No necesita rutas de la computadora del autor.

La preparación crea el contenedor `barberia-3-demo`, un volumen propio, la base `barberia_demo` y escucha solo en `127.0.0.1:3313`. Genera credenciales aleatorias y una clave AES privada en `.demo/` y `config.demo.properties`. No sobrescribe claves existentes. Para usar otro puerto, utilice una carpeta nueva y `--port 3318 --name barberia-3-otra`; repita siempre con ese mismo puerto/nombre. No modifique puertos de una instalación ya creada.

## Acceso de práctica

| Rol | Usuario | Clave de práctica |
|---|---|---|
| Administrador | admin.demo | AdminDemo2026! |
| Recepcionista | recepcion.demo | RecepDemo2026! |
| Barbero | barbero.demo | BarberoDemo2026! |

En el primer ingreso, vincule el QR con una aplicación Authenticator y escriba su código. Cada cuenta se vincula por separado. Estas claves son de cuentas ficticias de demostración, no credenciales del servidor. Para uso real cambie las claves, cree sus usuarios y mantenga privada la configuración. Nunca publique QR, secretos TOTP, respaldos ni datos personales.

## Tres ampliaciones principales

1. **Inventario y compras:** productos por código/categoría/precio, proveedores, entradas por lotes, vencimientos, stock mínimo, ajustes con motivo y consumo FEFO (vence primero, sale primero). No vende lotes vencidos ni existencias insuficientes.
2. **Punto de venta y caja:** carrito con varias líneas, descuentos, porcentaje de impuesto configurable según corresponda, efectivo/tarjeta/transferencia, cambio, factura consultable y PDF. Apertura, cierre y arqueo; citas y productos generan movimientos en la misma caja. Anulación/devolución completa autorizada, restituye stock y revierte puntos, sin borrar documentos. No admite anulaciones de cajas cerradas ni devoluciones parciales.
3. **Fidelización y seguimiento:** puntos persistentes por productos y servicios, niveles Bronce/Plata/Oro, canje controlado de 100 puntos por RD$100, recordatorios con prioridad/categoría/fecha/recurrencia, completar o posponer sin duplicar repeticiones. Son alertas dentro de la aplicación; no envía WhatsApp, correos ni notificaciones de un servicio externo.

## Operaciones y permisos

- Administrador: catálogo, proveedores, compras, lotes, horarios, usuarios, reportes, caja, auditoría y anulaciones.
- Recepcionista: clientes, citas, cobros, ventas, su caja, inventario de consulta, fidelización, recordatorios y reportes. No administra compras, proveedores, usuarios ni anulaciones de ventas.
- Barbero: agenda y estadísticas de sus citas, servicios y completar sus propias citas. No tiene directorio de clientes, inventario, ventas, caja ni reportes financieros.
- Los servicios vuelven a comprobar rol, cuenta activa y expiración de sesión; ocultar botones no sustituye la autorización.
- Para agregar un barbero: cree un usuario con rol BARBERO. Se crea su empleado, agenda y jornada inicial. El administrador ajusta Horarios, con días 1=lunes a 7=domingo. Los cambios no pueden dejar citas pendientes fuera de horario.

## Recorrido de demostración

1. Ingrese como administrador y revise indicadores, alertas, accesos rápidos y agenda por fecha.
2. En Proveedores registre uno. En Inventario cree un producto con código único y precio.
3. En Compras registre proveedor, producto, lote, vencimiento, cantidad y costo. Compruebe el stock en Inventario/Lotes.
4. En Caja abra con RD$500. En Ventas cree un carrito, seleccione cliente y método de pago. El importe se recalcula; el servicio vuelve a calcularlo con precios de la base.
5. Consulte la factura y exporte su PDF. Anule solo si corresponde y explique la devolución completa.
6. Cree una cita futura dentro del horario del barbero, complétela y registre el pago con caja abierta.
7. Revise Fidelización; cree y complete un recordatorio recurrente. Consulte auditoría como administrador.
8. Genere reportes de citas, ventas, inventario, caja y clientes; vista previa, impresión o PDF. El Excel de citas se conserva.
9. Cuente el efectivo, cierre su caja y revise la diferencia. Los pagos de tarjeta/transferencia no se suman al efectivo esperado.

Los reportes de ventas/caja/citas usan el período seleccionado. Inventario muestra las existencias actuales y Clientes el saldo acumulado. La impresión requiere una impresora disponible; el PDF no depende de ella. Las facturas son comprobantes académicos, sin integración fiscal ni numeración NCF certificada. El impuesto debe configurarlo el operador conforme al contexto; el sistema no decide obligaciones tributarias.

## Arquitectura y datos

- `controlador`: vistas JavaFX/FXML, formularios, navegación y presentación.
- `model`: clases de dominio heredadas del UML de la etapa inicial.
- `service`: reglas, permisos, cálculos, coordinación de transacciones y reportes.
- `dao/Store`: ejecución parametrizada, mapeo JDBC y commit/rollback; los servicios suministran las consultas.
- `db/Database`: pool, configuración externa y validación TLS para conexiones remotas.
- `security`: BCrypt, TOTP y cifrado de secretos; no se almacena la contraseña en texto plano.

SQL: `01_schema.sql` (estructura anterior), `02_demo.sql` (datos ficticios anteriores), `03_v3.sql` (migración aditiva) y `04_demo_v3.sql` (productos/lotes ficticios nuevos). La inicialización automática ejecuta los cuatro en orden. Para una base 2.0 existente: primero respaldo y prueba en copia; después `03_v3.sql` una sola vez, con permisos DDL y para crear/eliminar la rutina temporal. El script comprueba las columnas existentes. No importe datos de demostración sobre una base de producción. Un contenedor ya creado no vuelve a ejecutar los scripts de inicialización.

La conexión de MyASP.NET de la etapa anterior continúa pendiente de su certificado TLS. La versión 3.0 se verifica con MariaDB local aislada y MySQL de CI; no se desactiva la verificación TLS remota. No se afirma que la nube esté resuelta.

## Respaldo y restauración

Solo para la demo local que administra el propietario del equipo:

```bash
python scripts/respaldo.py crear backups/barberia.sql
```

El respaldo es privado y se crea sin sobrescribir otro archivo. Para comprobar una restauración, cree una instancia de ensayo separada y restaure allí:

```bash
python scripts/preparar-demo.py --profile restore --port 3317 --name barberia-v3-restore
python scripts/respaldo.py restaurar backups/barberia.sql --name barberia-v3-restore --confirmar-restauracion
```

La restauración puede reemplazar datos del destino: use una base vacía o su copia de ensayo, conserve antes un respaldo y compruebe tablas, conteos y acceso. Nunca restaure sobre una cuenta ajena o la base en uso. El script verifica la etiqueta del contenedor y no expone contraseñas en los argumentos. No suba respaldos a GitHub.

Para detener sin borrar datos: `podman stop barberia-3-demo` (o `docker stop`). Vuelva a ejecutar el preparador para iniciar. No elimine el volumen si quiere conservar sus registros y el vínculo de Authenticator; el cifrado requiere conservar también la clave de configuración.

## Pruebas y CI

Unitarias sin base:

```bash
mvn -B test "-Dtest=BusinessRulesTest,SecurityTest,EnterpriseRulesTest"
```

Integración y JavaFX: exclusivamente con base ficticia separada. No active estos indicadores contra producción; la GUI reinicia el 2FA de las tres cuentas demo.

```bash
python scripts/preparar-demo.py --profile test --port 3315 --name barberia-v3-test
```

En la configuración privada de ensayo añada las propiedades de claves demo indicadas en la plantilla. Ejemplo Linux:

```bash
BARBERIA_DB_TEST=true BARBERIA_UI=true mvn -B verify -Dbarberia.config=config.test.properties
```

En CI se utiliza Xvfb; localmente necesita una sesión gráfica. En Windows defina los dos indicadores como variables de entorno desde PowerShell. Resultados: `target/surefire-reports/`; capturas: `target/evidencias/`.

El workflow compila con Java21 en Linux y Windows, ejecuta las 20 pruebas unitarias, verifica integración MySQL y JavaFX y solo publica si los trabajos pasan. Publica la Release `v3.0.1` una sola vez sin reemplazar archivos de una versión publicada. Los ZIP incluyen ejecutable, SQL, preparación y manual; excluyen configuración privada, usuarios reales, logs y respaldos. Una instalación gráfica en otro equipo sigue siendo una validación distinta de compilar en CI.

## Revisión del plan del profesor

Consulte `docs/MATRIZ_REQUISITOS.md` para la adaptación por estudiante y sus límites. No se incorporan módulos escolares, medicamentos regulados ni una mascota virtual que no corresponde al negocio. Sus conceptos útiles se aplican a permisos, horarios, FEFO, trazabilidad, cierres, recordatorios y niveles de fidelidad.

Se utilizó Codex como apoyo en implementación, pruebas y documentación. Los resultados se verifican con ejecuciones reales; el estudiante debe revisar el código y explicar sus decisiones con su propia voz.

La entrega final de la serie 3.0 es 3.0.1: ajusta el tamaño del carrito y de los modales y permite editar seguimientos pendientes. La publicación 3.0.0 se conserva en el historial.
