-- Datos ficticios de práctica 3.0. No representan compras ni clientes reales.
INSERT IGNORE INTO proveedores(nombre,telefono) VALUES('Distribuidora Demo','809-555-0200');
INSERT IGNORE INTO productos(codigo,nombre,categoria,precio,minimo) VALUES('CERA-01','Cera de acabado mate','Estilismo',350,3),('GEL-01','Gel fijador profesional','Estilismo',450,3),('ACEITE-01','Aceite para barba','Cuidado de barba',600,3);
START TRANSACTION;
INSERT INTO compras(proveedor_id,usuario_id,total) SELECT p.id,u.id,2000 FROM proveedores p JOIN usuarios u ON u.usuario='admin.demo' WHERE p.nombre='Distribuidora Demo' AND NOT EXISTS(SELECT 1 FROM lotes WHERE codigo='DEMO-CERA-01');
SET @compra=LAST_INSERT_ID();
INSERT INTO lotes(producto_id,compra_id,codigo,vencimiento,costo,cantidad) SELECT id,@compra,'DEMO-CERA-01',DATE_ADD(CURRENT_DATE,INTERVAL 180 DAY),100,20 FROM productos WHERE codigo='CERA-01' AND NOT EXISTS(SELECT 1 FROM lotes WHERE codigo='DEMO-CERA-01');
INSERT INTO compras(proveedor_id,usuario_id,total) SELECT p.id,u.id,3000 FROM proveedores p JOIN usuarios u ON u.usuario='admin.demo' WHERE p.nombre='Distribuidora Demo' AND NOT EXISTS(SELECT 1 FROM lotes WHERE codigo='DEMO-GEL-01');
SET @compra=LAST_INSERT_ID();
INSERT INTO lotes(producto_id,compra_id,codigo,vencimiento,costo,cantidad) SELECT id,@compra,'DEMO-GEL-01',DATE_ADD(CURRENT_DATE,INTERVAL 240 DAY),150,20 FROM productos WHERE codigo='GEL-01' AND NOT EXISTS(SELECT 1 FROM lotes WHERE codigo='DEMO-GEL-01');
INSERT INTO compras(proveedor_id,usuario_id,total) SELECT p.id,u.id,5000 FROM proveedores p JOIN usuarios u ON u.usuario='admin.demo' WHERE p.nombre='Distribuidora Demo' AND NOT EXISTS(SELECT 1 FROM lotes WHERE codigo='DEMO-ACEITE-01');
SET @compra=LAST_INSERT_ID();
INSERT INTO lotes(producto_id,compra_id,codigo,vencimiento,costo,cantidad) SELECT id,@compra,'DEMO-ACEITE-01',DATE_ADD(CURRENT_DATE,INTERVAL 120 DAY),250,20 FROM productos WHERE codigo='ACEITE-01' AND NOT EXISTS(SELECT 1 FROM lotes WHERE codigo='DEMO-ACEITE-01');
COMMIT;
