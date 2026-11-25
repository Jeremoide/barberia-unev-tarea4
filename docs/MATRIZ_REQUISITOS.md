# Matriz de la versión 3.0

Fuente: Plan de mejora integral de Programación II UNEV. Se adaptan procesos a una barbería, con datos ficticios persistidos para la demostración. Las evidencias y el informe académico se conservan fuera de la carpeta del proyecto.

## Frontend obligatorio

| Requisito | Implementación |
|---|---|
| Dashboard con indicadores, estadísticas, alertas y accesos rápidos | Citas del período, clientes atendidos, cobros, gráficos, stock bajo, unidades vencidas, seguimientos y ventas de hoy; accesos a venta/caja/agenda |
| Navegación con iconos y sección activa | Menú lateral desplazable, SVG vectoriales, sección activa; menú compacto en ancho pequeño |
| Modales crear, editar, consultar, confirmar/anular | Editor de clientes/servicios/citas/usuarios, producto, proveedor, compra, venta, horario, detalle y confirmaciones |
| TableView y acciones por fila | Tablas ordenables, selección, consulta por fila y acciones de módulo |
| Búsqueda dinámica y filtros combinados | Debounce, texto, período y estado; nombres/códigos/categorías según módulo |
| Paginación persistente | 30 registros por página con LIMIT/OFFSET, botones anterior/siguiente |
| Formularios y controles adecuados | DatePicker, ComboBox, Spinner, secciones, GridPane y validación de formatos/valores |
| Validación en línea, duplicados y mensajes | Campos vacíos resaltados, formatos, restricciones únicas, errores de backend visibles, botón bloqueado al guardar y al no seleccionar |
| CSS, tipografía y redimensionamiento | Tema azul noche de la referencia, contraste claro, paneles, formularios con desplazamiento y adaptación del menú |
| Teclado y prevención de duplicados | Controles nativos enfocables, labels asociados, botón guardar deshabilitado durante la operación, restricciones únicas y bloqueos transaccionales |
| Reportes, vista previa, impresión y PDF | Citas, ventas, inventario, caja y clientes; Excel de citas adicional |
| Estados e indicadores gráficos | Estado de citas/ventas/recordatorios, gráficas, alertas, puntos y nivel de cliente |

La tabla ordena la página cargada; no se afirma ordenamiento global por cualquier columna de todas las páginas. Se probaron capturas de 1366×768, 1024×768, 768×1024 y 412×915; las tablas anchas tienen desplazamiento horizontal y los modales desplazamiento vertical.

## Backend obligatorio

| Requisito | Implementación |
|---|---|
| POO y capas | Controladores, modelos, servicios, DAO Store y Database; consultas coordinadas por servicios y ejecución parametrizada en DAO |
| Autenticación/cierre/roles | Administrador, recepcionista y barbero; permisos revalidados en servicios, cuenta activa y sesión expirada |
| Hash y credenciales | BCrypt y secretos TOTP cifrados; propiedades privadas, demo con claves aleatorias para la base |
| JDBC parametrizado | PreparedStatement en Store; parámetros del usuario separados del SQL |
| Reglas de negocio | Horarios/cruces de citas, pago único, stock/vencimiento, importes, descuento, caja, canjes y recurrencias |
| PK/FK/restricciones/índices | Esquema inicial y migración 3.0, índices por fecha, lote y claves relacionadas |
| Transacciones | Compras/lotes, ventas/detalles/stock/caja/puntos, anulaciones y cobros de citas |
| Auditoría | Actor, operación, referencia y fecha; accesos y operaciones de negocio |
| Anulación y eliminación lógica | Clientes/servicios/productos desactivados; venta anulada permanece con detalles y motivo |
| Excepciones | Mensajes de negocio claros; SQL sin datos privados en pantalla; logs locales privados |
| Respaldos y restauración | Scripts de respaldo y restauración de demo; restauración real en contenedor separado y comparación de conteos |
| Consultas eficientes/paginación | LIMIT/OFFSET, índices y filtros SQL; carga asíncrona para no bloquear JavaFX |
| Reportes persistentes | Consultas reales a la base seleccionada, sin estadísticas inventadas en la interfaz |
| JUnit y funcionales | Reglas, integración, permisos, escenarios negativos, reversión y UI real |
| GitHub y ejecutable | Historial conservado desde v2, rama 3.0, workflow de compilación/pruebas/publicación y ZIP por plataforma |

## Adaptaciones de las observaciones individuales

| Proyecto de referencia | Aplicación a la barbería | Elementos sin aplicación directa |
|---|---|---|
| Digenelis académico | Usuarios/roles, estructura de módulos, horarios, validaciones, cálculo de totales, cierres y reportes | Matrícula, períodos escolares, evaluaciones, notas y boletines no corresponden a la barbería |
| Enmanuel agroveterinaria | Productos, categorías, proveedores, compras, lotes/vencimientos, stock, ventas, métodos de pago, apertura/cierre, devoluciones completas, auditoría y respaldo | No se incorpora gestión veterinaria |
| Vladimir farmacia | Inventario por lotes, FEFO, bloqueo de vencidos, facturación independiente, caja, arqueo, filtros y trazabilidad | Medicamentos y controles sanitarios/regulados no corresponden; no se afirma cumplimiento farmacéutico |
| Eliam colegio | Acceso por perfil, relación usuario-barbero, horarios propios, bloqueo por permisos, historial y procesos cerrados | Padres, estudiantes, aula virtual, promoción y notas no corresponden |
| Duarte David POS | Rediseño con menú/iconos, dashboard, módulos, carrito, cantidades, descuento/impuesto, pago/cambio, comprobante, caja, historial y ejecutable | Comprobante académico; sin NCF fiscal certificado ni devoluciones parciales. Instalación en otro equipo requiere ensayo presencial |
| Wildania recordatorios/avatar | Seguimientos por cliente, fecha, categoría, prioridad, recurrencia, completar/posponer, alertas, puntos, canje, niveles y persistencia sin recompensas duplicadas | No se agrega una mascota que se alimente ni la web de otro proyecto; los niveles representan fidelidad comercial. Alertas dentro de la app, sin mensajería externa |

## Tres ampliaciones destacadas

1. Inventario trazable con proveedores, compras, vencimientos, FEFO y ajustes auditados.
2. Punto de venta transaccional integrado a caja y cobros de servicios, con arqueo y devolución completa.
3. Fidelización persistente con puntos/canjes/niveles y agenda de seguimiento recurrente.

## Límites de verificación

La impresión requiere una impresora; se verifica vista previa y exportación PDF, no salida física. Las cuentas y productos de demo son ficticios. La validación en otra computadora y la explicación oral son actividades del estudiante. La incidencia TLS del hosting anterior permanece separada y no se elude.
