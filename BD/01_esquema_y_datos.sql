-- =====================================================================
--  Práctica 2 RMI - Sistema Distribuido de Reservas e Inventario de Laboratorios
--  Capa 3 (Persistencia) - Esquema completo para MySQL 8.x
--  ATENCIÓN: este script BORRA y vuelve a crear las tablas (deja todo limpio).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS laboratorio_rmi
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE laboratorio_rmi;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS bitacora;
DROP TABLE IF EXISTS reservas;
DROP TABLE IF EXISTS equipos;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 1) INVENTARIO: las 20 computadoras del laboratorio (plano 4 filas x 5 columnas)
--    El estado "EN_PROCESO" NO se guarda aquí: es un bloqueo temporal que vive
--    en la memoria del servidor (si el servidor se reinicia, desaparece solo).
-- ---------------------------------------------------------------------
CREATE TABLE equipos (
  id_equipo        INT          NOT NULL,
  nombre           VARCHAR(20)  NOT NULL,
  fila             TINYINT      NOT NULL,
  columna          TINYINT      NOT NULL,
  especificaciones VARCHAR(120) NULL,
  estado           ENUM('DISPONIBLE','RESERVADO','MANTENIMIENTO') NOT NULL DEFAULT 'DISPONIBLE',
  actualizado_en   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT pk_equipos       PRIMARY KEY (id_equipo),
  CONSTRAINT uk_equipo_nombre UNIQUE (nombre),
  CONSTRAINT uk_equipo_pos    UNIQUE (fila, columna),
  CONSTRAINT chk_equipo_id    CHECK (id_equipo BETWEEN 1 AND 20),
  CONSTRAINT chk_equipo_fila  CHECK (fila BETWEEN 1 AND 4),
  CONSTRAINT chk_equipo_col   CHECK (columna BETWEEN 1 AND 5)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 2) RESERVAS / TICKETS (comprobantes generados por el servidor)
--    "equipo_activo" es una columna calculada: vale id_equipo solo cuando la
--    reserva está ACTIVA (si no, NULL). Con un UNIQUE sobre ella, la propia
--    base de datos impide que existan DOS reservas activas del mismo equipo.
--    Es la "última línea de defensa" contra la doble reserva.
-- ---------------------------------------------------------------------
CREATE TABLE reservas (
  id_reserva          BIGINT       NOT NULL AUTO_INCREMENT,
  codigo_confirmacion VARCHAR(40)  NOT NULL,
  id_alumno           VARCHAR(20)  NOT NULL,
  id_equipo           INT          NOT NULL,
  hora_inicio         DATETIME     NOT NULL,
  hora_fin            DATETIME     NOT NULL,
  marca_tiempo        DATETIME(3)  NOT NULL,
  firma_sha256        CHAR(64)     NOT NULL,
  cliente_host        VARCHAR(64)  NULL,
  estado              ENUM('ACTIVA','CANCELADA') NOT NULL DEFAULT 'ACTIVA',
  cancelada_en        DATETIME(3)  NULL,
  equipo_activo       INT GENERATED ALWAYS AS
                        (CASE WHEN estado = 'ACTIVA' THEN id_equipo ELSE NULL END) STORED,
  CONSTRAINT pk_reservas           PRIMARY KEY (id_reserva),
  CONSTRAINT uk_reserva_codigo     UNIQUE (codigo_confirmacion),
  CONSTRAINT uk_una_activa_x_equipo UNIQUE (equipo_activo),
  CONSTRAINT fk_reserva_equipo     FOREIGN KEY (id_equipo) REFERENCES equipos (id_equipo),
  CONSTRAINT chk_reserva_horario   CHECK (hora_fin > hora_inicio),
  INDEX idx_reserva_alumno (id_alumno)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 3) BITÁCORA: registro de TODO lo que pasa (éxitos, rechazos, expiraciones)
-- ---------------------------------------------------------------------
CREATE TABLE bitacora (
  id_evento    BIGINT       NOT NULL AUTO_INCREMENT,
  fecha        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  id_alumno    VARCHAR(20)  NULL,
  id_equipo    INT          NULL,
  accion       VARCHAR(30)  NOT NULL,
  resultado    ENUM('EXITO','RECHAZADO','ERROR','INFO') NOT NULL,
  detalle      VARCHAR(255) NULL,
  cliente_host VARCHAR(64)  NULL,
  CONSTRAINT pk_bitacora        PRIMARY KEY (id_evento),
  CONSTRAINT fk_bitacora_equipo FOREIGN KEY (id_equipo) REFERENCES equipos (id_equipo),
  INDEX idx_bitacora_fecha (fecha),
  INDEX idx_bitacora_equipo (id_equipo)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 4) DATOS INICIALES: 20 equipos; PC-04, PC-13 y PC-18 en mantenimiento
-- ---------------------------------------------------------------------
INSERT INTO equipos (id_equipo, nombre, fila, columna, especificaciones, estado) VALUES
 ( 1,'PC-01',1,1,'Core i5 / 16 GB / SSD 512 GB','DISPONIBLE'),
 ( 2,'PC-02',1,2,'Core i5 / 16 GB / SSD 512 GB','DISPONIBLE'),
 ( 3,'PC-03',1,3,'Core i5 / 16 GB / SSD 512 GB','DISPONIBLE'),
 ( 4,'PC-04',1,4,'Core i5 / 16 GB / SSD 512 GB','MANTENIMIENTO'),
 ( 5,'PC-05',1,5,'Core i5 / 16 GB / SSD 512 GB','DISPONIBLE'),
 ( 6,'PC-06',2,1,'Core i7 / 32 GB / SSD 1 TB','DISPONIBLE'),
 ( 7,'PC-07',2,2,'Core i7 / 32 GB / SSD 1 TB','DISPONIBLE'),
 ( 8,'PC-08',2,3,'Core i7 / 32 GB / SSD 1 TB','DISPONIBLE'),
 ( 9,'PC-09',2,4,'Core i7 / 32 GB / SSD 1 TB','DISPONIBLE'),
 (10,'PC-10',2,5,'Core i7 / 32 GB / SSD 1 TB','DISPONIBLE'),
 (11,'PC-11',3,1,'Ryzen 7 / 32 GB / SSD 1 TB','DISPONIBLE'),
 (12,'PC-12',3,2,'Ryzen 7 / 32 GB / SSD 1 TB','DISPONIBLE'),
 (13,'PC-13',3,3,'Ryzen 7 / 32 GB / SSD 1 TB','MANTENIMIENTO'),
 (14,'PC-14',3,4,'Ryzen 7 / 32 GB / SSD 1 TB','DISPONIBLE'),
 (15,'PC-15',3,5,'Ryzen 7 / 32 GB / SSD 1 TB','DISPONIBLE'),
 (16,'PC-16',4,1,'Core i9 / 64 GB / RTX 4060','DISPONIBLE'),
 (17,'PC-17',4,2,'Core i9 / 64 GB / RTX 4060','DISPONIBLE'),
 (18,'PC-18',4,3,'Core i9 / 64 GB / RTX 4060','MANTENIMIENTO'),
 (19,'PC-19',4,4,'Core i9 / 64 GB / RTX 4060','DISPONIBLE'),
 (20,'PC-20',4,5,'Core i9 / 64 GB / RTX 4060','DISPONIBLE');

INSERT INTO bitacora (id_alumno, id_equipo, accion, resultado, detalle, cliente_host)
VALUES (NULL, NULL, 'INICIALIZACION', 'INFO', 'Esquema creado con 20 equipos (3 en mantenimiento)', 'script-sql');

-- Comprobación rápida
SELECT estado, COUNT(*) AS total FROM equipos GROUP BY estado;
