-- Datos de práctica. Importar una sola vez en una base vacía después de 01_schema.sql.
-- No contiene secretos 2FA ni credenciales de conexión.

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
INSERT INTO `usuarios` (`id`, `nombre`, `usuario`, `correo`, `password_hash`, `rol_id`, `activo`, `fallos`, `bloqueado_hasta`, `creado`) VALUES (1,'Administrador Demo','admin.demo','admin.demo@example.com','$2a$12$6DvM7raEY/lpebAa40gYauRmX01oke/IfRzBQ70J5.Cd3W3Ic2PTK',1,1,0,NULL,'2026-11-21 03:36:18');
INSERT INTO `usuarios` (`id`, `nombre`, `usuario`, `correo`, `password_hash`, `rol_id`, `activo`, `fallos`, `bloqueado_hasta`, `creado`) VALUES (2,'Recepción Demo','recepcion.demo','recepcion.demo@example.com','$2a$12$iP.xJLiOkwmxQmQejpUfe.PYdZmlEPMSCHw/.3SqSPSUzMlsv1sq6',2,1,0,NULL,'2026-11-21 03:36:18');
INSERT INTO `usuarios` (`id`, `nombre`, `usuario`, `correo`, `password_hash`, `rol_id`, `activo`, `fallos`, `bloqueado_hasta`, `creado`) VALUES (3,'Carlos Barbero','barbero.demo','barbero.demo@example.com','$2a$12$6JaXSQVDSVSIVFkox4hRZOYpEBDlSvVauqj.0/9VvQIEg1WqNeWHy',3,1,0,NULL,'2026-11-21 03:36:19');
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
INSERT INTO `clientes` (`id`, `nombre`, `telefono`, `correo`, `creado`) VALUES (1,'Ana Demo','809-555-0100','cliente0@example.com','2026-11-21 03:36:19');
INSERT INTO `clientes` (`id`, `nombre`, `telefono`, `correo`, `creado`) VALUES (2,'Luis Demo','809-555-0101','cliente1@example.com','2026-11-21 03:36:19');
INSERT INTO `clientes` (`id`, `nombre`, `telefono`, `correo`, `creado`) VALUES (3,'Pedro Demo','809-555-0102','cliente2@example.com','2026-11-21 03:36:19');
INSERT INTO `clientes` (`id`, `nombre`, `telefono`, `correo`, `creado`) VALUES (4,'María Demo','809-555-0103','cliente3@example.com','2026-11-21 03:36:19');
INSERT INTO `clientes` (`id`, `nombre`, `telefono`, `correo`, `creado`) VALUES (5,'José Demo','809-555-0104','cliente4@example.com','2026-11-21 03:36:19');
INSERT INTO `clientes` (`id`, `nombre`, `telefono`, `correo`, `creado`) VALUES (6,'Clara Demo','809-555-0105','cliente5@example.com','2026-11-21 03:36:19');
INSERT INTO `clientes` (`id`, `nombre`, `telefono`, `correo`, `creado`) VALUES (7,'Rafael Demo','809-555-0106','cliente6@example.com','2026-11-21 03:36:19');
INSERT INTO `clientes` (`id`, `nombre`, `telefono`, `correo`, `creado`) VALUES (8,'Lucía Demo','809-555-0107','cliente7@example.com','2026-11-21 03:36:19');
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
INSERT INTO `empleados` (`id`, `usuario_id`, `nombre`, `especialidad`) VALUES (1,3,'Carlos Barbero','Corte y barba');
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
INSERT INTO `categorias_servicio` (`id`, `nombre`, `descripcion`) VALUES (1,'Cortes','Servicios de cabello');
INSERT INTO `categorias_servicio` (`id`, `nombre`, `descripcion`) VALUES (2,'Barba','Cuidado de barba');
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
INSERT INTO `servicios` (`id`, `nombre`, `precio`, `duracion_minutos`, `categoria_id`) VALUES (1,'Corte clásico',350.00,30,1);
INSERT INTO `servicios` (`id`, `nombre`, `precio`, `duracion_minutos`, `categoria_id`) VALUES (2,'Corte y barba',550.00,45,2);
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
INSERT INTO `agendas` (`id`, `empleado_id`) VALUES (1,1);
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
INSERT INTO `horarios_trabajo` (`id`, `empleado_id`, `dia_semana`, `hora_inicio`, `hora_fin`) VALUES (1,1,1,'08:00:00','20:00:00');
INSERT INTO `horarios_trabajo` (`id`, `empleado_id`, `dia_semana`, `hora_inicio`, `hora_fin`) VALUES (2,1,2,'08:00:00','20:00:00');
INSERT INTO `horarios_trabajo` (`id`, `empleado_id`, `dia_semana`, `hora_inicio`, `hora_fin`) VALUES (3,1,3,'08:00:00','20:00:00');
INSERT INTO `horarios_trabajo` (`id`, `empleado_id`, `dia_semana`, `hora_inicio`, `hora_fin`) VALUES (4,1,4,'08:00:00','20:00:00');
INSERT INTO `horarios_trabajo` (`id`, `empleado_id`, `dia_semana`, `hora_inicio`, `hora_fin`) VALUES (5,1,5,'08:00:00','20:00:00');
INSERT INTO `horarios_trabajo` (`id`, `empleado_id`, `dia_semana`, `hora_inicio`, `hora_fin`) VALUES (6,1,6,'08:00:00','20:00:00');
INSERT INTO `horarios_trabajo` (`id`, `empleado_id`, `dia_semana`, `hora_inicio`, `hora_fin`) VALUES (7,1,7,'08:00:00','20:00:00');
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (1,1,1,1,1,'2026-11-12 09:00:00',350.00,30,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (2,2,1,2,1,'2026-11-12 11:00:00',550.00,45,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (3,3,1,1,1,'2026-11-12 13:00:00',350.00,30,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (4,4,1,2,1,'2026-11-13 09:00:00',550.00,45,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (5,5,1,1,1,'2026-11-13 11:00:00',350.00,30,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (6,6,1,2,1,'2026-11-13 13:00:00',550.00,45,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (7,7,1,1,1,'2026-11-14 09:00:00',350.00,30,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (8,8,1,2,1,'2026-11-14 11:00:00',550.00,45,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (9,1,1,1,1,'2026-11-14 13:00:00',350.00,30,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (10,2,1,2,1,'2026-11-15 09:00:00',550.00,45,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (11,3,1,1,1,'2026-11-15 11:00:00',350.00,30,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (12,4,1,2,1,'2026-11-15 13:00:00',550.00,45,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (13,5,1,1,1,'2026-11-16 09:00:00',350.00,30,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (14,6,1,2,1,'2026-11-16 11:00:00',550.00,45,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (15,7,1,1,1,'2026-11-16 13:00:00',350.00,30,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (16,8,1,2,1,'2026-11-17 09:00:00',550.00,45,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (17,1,1,1,1,'2026-11-17 11:00:00',350.00,30,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (18,2,1,2,1,'2026-11-17 13:00:00',550.00,45,'COMPLETADA');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (19,3,1,1,1,'2026-11-22 09:00:00',350.00,30,'PENDIENTE');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (20,4,1,2,1,'2026-11-22 11:00:00',550.00,45,'PENDIENTE');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (21,5,1,1,1,'2026-11-22 13:00:00',350.00,30,'PENDIENTE');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (22,6,1,2,1,'2026-11-23 09:00:00',550.00,45,'PENDIENTE');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (23,7,1,1,1,'2026-11-23 11:00:00',350.00,30,'PENDIENTE');
INSERT INTO `citas` (`id`, `cliente_id`, `empleado_id`, `servicio_id`, `agenda_id`, `fecha`, `precio_acordado`, `duracion_acordada`, `estado`) VALUES (24,8,1,2,1,'2026-11-23 13:00:00',550.00,45,'PENDIENTE');
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (1,1,350.00,'2026-11-12 09:30:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (2,2,550.00,'2026-11-12 11:45:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (3,3,350.00,'2026-11-12 13:30:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (4,4,550.00,'2026-11-13 09:45:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (5,5,350.00,'2026-11-13 11:30:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (6,6,550.00,'2026-11-13 13:45:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (7,7,350.00,'2026-11-14 09:30:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (8,8,550.00,'2026-11-14 11:45:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (9,9,350.00,'2026-11-14 13:30:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (10,10,550.00,'2026-11-15 09:45:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (11,11,350.00,'2026-11-15 11:30:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (12,12,550.00,'2026-11-15 13:45:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (13,13,350.00,'2026-11-16 09:30:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (14,14,550.00,'2026-11-16 11:45:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (15,15,350.00,'2026-11-16 13:30:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (16,16,550.00,'2026-11-17 09:45:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (17,17,350.00,'2026-11-17 11:30:00');
INSERT INTO `pagos` (`id`, `cita_id`, `monto`, `fecha`) VALUES (18,18,550.00,'2026-11-17 13:45:00');
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

