-- Esquema de la base. MySQL lo ejecuta automáticamente la primera vez que
-- se crea el contenedor (docker-entrypoint-initdb.d).

CREATE TABLE IF NOT EXISTS tarea (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    titulo         VARCHAR(120)  NOT NULL,
    descripcion    VARCHAR(1000),
    estado         VARCHAR(20)   NOT NULL,
    prioridad      VARCHAR(10)   NOT NULL,
    creada_en      DATETIME(6)   NOT NULL,
    actualizada_en DATETIME(6)   NOT NULL,
    version        BIGINT        NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    INDEX idx_tarea_estado (estado),

    -- La base también se defiende: aunque alguien escriba directo en la
    -- tabla, no puede guardar un estado o prioridad que no existe.
    CONSTRAINT chk_tarea_estado    CHECK (estado    IN ('PENDIENTE', 'EN_CURSO', 'HECHA', 'CANCELADA')),
    CONSTRAINT chk_tarea_prioridad CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
