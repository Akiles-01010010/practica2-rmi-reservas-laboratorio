-- Deja TODAS las reservas canceladas y los equipos reservados vuelven a DISPONIBLE.
-- Útil para repetir las pruebas sin perder la bitácora (no toca el mantenimiento).
USE laboratorio_rmi;
START TRANSACTION;
UPDATE reservas SET estado = 'CANCELADA', cancelada_en = NOW(3) WHERE estado = 'ACTIVA';
UPDATE equipos  SET estado = 'DISPONIBLE' WHERE estado = 'RESERVADO';
INSERT INTO bitacora (accion, resultado, detalle, cliente_host)
VALUES ('REINICIO_RESERVAS', 'INFO', 'Reservas activas canceladas por script', 'script-sql');
COMMIT;
SELECT estado, COUNT(*) AS total FROM equipos GROUP BY estado;
