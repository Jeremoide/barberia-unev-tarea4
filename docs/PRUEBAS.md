# Verificación de Barbería 3.0

Ejecución local con base MariaDB de ensayo separada, JUnit5 y sesión JavaFX real. Los registros utilizados son ficticios. Los casos de integración crean sus propios códigos para evitar colisiones. Los informes XML y capturas se generan en target, no se guardan como credenciales o pruebas inventadas en el repositorio.

## Casos de integración nuevos

| Caso | Entrada o condición | Resultado comprobado |
|---|---|---|
| Compra y existencias | 20 unidades a RD$80 | Lote con 20 unidades persistido |
| Permisos | Barbero intenta caja y crear producto | SecurityException, no operación |
| Venta y caja | Fondo RD$500, venta 2×RD$200, recibido RD$500 | Stock 18, movimiento de RD$400, 4 puntos |
| Sin stock | Solicitar 50 unidades con 18 disponibles | Rechazo; venta y stock no cambian |
| Pago insuficiente | Producto RD$200, recibido RD$1 | Rechazo transaccional |
| Canje insuficiente | Canjear 100 puntos con saldo menor | Rechazo; saldo conservado |
| Devolución completa | Anular la primera venta con motivo | Stock vuelve a 20, saldo puntos cero e historial ANULADA; segunda anulación rechazada |
| Vencimiento y FEFO | Lote antiguo vencido y dos lotes vigentes | No vende el vencido; consume primero el que vence antes |
| Arqueo | Venta con tarjeta y fondo RD$500 | Contado RD$500, diferencia cero; caja cerrada impide nuevas ventas |
| Recurrencia | Completar seguimiento repetible cada siete días | Una siguiente actividad; repetir completar no crea otra |
| Reportes y auditoría | Consultar cuatro reportes y referencia de venta | Resultados desde base, PDF exportados y operación registrada |
| Baja lógica | Desactivar cliente con ventas | Cliente y documentos permanecen, activo=0 |

## Otras comprobaciones

- 20 unitarias: nombres y fechas, TOTP/cifrado/permisos y diez reglas de importes, redondeo, cantidades, descuentos e impuestos.
- Cuatro integraciones anteriores: acceso con doble factor, roles/Excel, clientes persistentes y ciclo de citas/cobros.
- Recorrido JavaFX: acceso, 2FA, dashboard y clientes en cuatro tamaños, formulario inválido, módulos nuevos, carrito y perfiles recepcionista/barbero.
- Respaldo restaurado en otra instancia y conteos comparados; no se considera suficiente que el archivo de respaldo exista.
- Exportación PDF comprobable sin impresora. La impresión física y la instalación en otro equipo deben verificarse en el entorno del estudiante.

Los pasos exactos para repetir están en README. Se conserva la ejecución remota en GitHub Actions; su resultado debe consultarse en la rama de esta versión. Una ejecución fallida no se presenta como aprobada.
