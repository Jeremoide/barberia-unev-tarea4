# Manual de usuario de Barbería 3.0

## Iniciar

Prepare y abra la demo siguiendo el Inicio rápido del README. Ingrese con la cuenta de práctica de su rol y vincule Authenticator en el primer acceso. No comparta el QR ni la clave de la base. Use Cerrar sesión al terminar.

## Agenda y clientes

En Clientes cree o edite nombre, teléfono y correo. La búsqueda filtra al escribir. Desactivar conserva las citas y los documentos anteriores. En Servicios, el administrador configura precio, duración y categoría. En Citas seleccione cliente, barbero, servicio y fecha/hora futura. El sistema rechaza conflictos y fuera de horario. Complete solo servicios realizados; cancelar conserva la cita. El barbero solo completa citas propias.

## Inventario y compras

El administrador registra el proveedor y crea el producto con código único, categoría, precio y mínimo. En Compras selecciona proveedor/producto y registra lote, vencimiento, cantidad y costo. Inventario muestra stock disponible y vencido; Lotes permite consultar fechas y, al administrador, ajustar una existencia con motivo. La venta descuenta primero el lote que vence antes. Un producto con existencias no se desactiva hasta resolverlas.

## Venta y caja

Abra su caja con el fondo contado. En Ventas pulse Nueva venta, seleccione cliente, añada productos y cantidades al carrito y revise descuento, impuesto y total. El impuesto es un porcentaje configurable, no una determinación fiscal automática. Seleccione efectivo, tarjeta o transferencia; en efectivo registre cuánto recibió para calcular el cambio. Pulse cobrar una vez. Si aparece un error, el formulario permanece abierto y el backend revierte la transacción.

Consulte la factura desde la fila de la venta. Puede exportarla a PDF o imprimirla. El administrador puede anular/devolver la venta completa con un motivo mientras la caja siga abierta. Se restaura el stock y se revierten los puntos. Una venta con puntos ya consumidos no se anula si dejaría saldo negativo. No hay devoluciones parciales ni modificación de cierres históricos.

Para cobrar una cita completada también debe tener caja abierta; el pago queda vinculado al movimiento y a los puntos del cliente. Al cerrar, cuente solo el efectivo físico, escriba el importe y revise la diferencia. Tarjeta y transferencia no aumentan el efectivo esperado.

## Fidelización y recordatorios

Fidelización muestra puntos y nivel acumulados: Bronce, Plata desde 30 puntos y Oro desde 100. Cada RD$100 cobrados genera un punto entero. En la venta puede canjear 100 puntos por RD$100 de descuento si el saldo y el total lo permiten. Las devoluciones revierten los puntos de esa operación.

En Recordatorios registre cliente, actividad, categoría, prioridad, fecha/hora y repetición en días. Cero significa sin repetición. Editar permite cambiar una actividad pendiente. Completar cierra la actividad y crea la siguiente si corresponde; posponer conserva el historial y crea una nueva para el día siguiente. La aplicación muestra alertas locales en el dashboard y la lista. No envía mensajes fuera del programa.

## Reportes y administración

En Reportes seleccione citas, ventas, inventario, caja o clientes. Revise el período cuando aplique, abra la vista previa y exporte PDF o imprima. El encabezado conserva Excel de citas. Usuarios permite crear cuentas y reiniciar la vinculación de Authenticator; Horarios permite configurar las jornadas de los barberos; Auditoría registra las operaciones y Accesos los eventos de autenticación.

Para copias de seguridad y restauración siga los comandos del README. Mantenga los respaldos y la clave de cifrado privados. El manual no autoriza restaurar sobre una base ajena o reemplazar datos reales sin respaldo.
