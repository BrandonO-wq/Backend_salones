-- ============================================================
-- Tabla: users
-- Descripción: Almacena los usuarios registrados en la plataforma.
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name          TEXT        NOT NULL,
    email         TEXT        NOT NULL,
    phone         TEXT,
    password_hash TEXT        NOT NULL,
    roles         TEXT        NOT NULL DEFAULT 'CLIENT',  -- roles separados por coma, ej: 'CLIENT,HOST'
    fb_token      TEXT,                                   -- token de firebase para notificaciones (uso futuro)
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Índice único para garantizar que no existan emails duplicados
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users (email);
