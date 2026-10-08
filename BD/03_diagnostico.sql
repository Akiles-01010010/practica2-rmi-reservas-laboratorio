-- Diagnóstico para la PRUEBA 2 (desconexión) y la PRUEBA 3 (reinicio)
USE laboratorio_rmi;

SELECT '--- 1) Transacciones abiertas en InnoDB (debe salir VACÍO) ---' AS paso;
SELECT trx_id, trx_state, trx_started, trx_rows_locked, trx_mysql_thread_id
FROM information_schema.innodb_trx;

SELECT '--- 2) Filas bloqueadas en este momento (debe salir VACÍO) ---' AS paso;
SELECT engine_transaction_id, object_name, lock_type, lock_mode, lock_status
FROM performance_schema.data_locks
WHERE object_schema = 'laboratorio_rmi';

SELECT '--- 3) Estado del inventario ---' AS paso;
SELECT e.id_equipo, e.nombre, e.estado, r.id_alumno, r.codigo_confirmacion,
       DATE_FORMAT(r.hora_inicio,'%H:%i') AS inicio, DATE_FORMAT(r.hora_fin,'%H:%i') AS fin
FROM equipos e LEFT JOIN reservas r ON r.equipo_activo = e.id_equipo
ORDER BY e.id_equipo;

SELECT '--- 4) Últimos 15 eventos de la bitácora ---' AS paso;
SELECT id_evento, DATE_FORMAT(fecha,'%H:%i:%s.%f') AS hora, id_alumno, id_equipo,
       accion, resultado, LEFT(detalle,70) AS detalle, cliente_host
FROM bitacora ORDER BY id_evento DESC LIMIT 15;
