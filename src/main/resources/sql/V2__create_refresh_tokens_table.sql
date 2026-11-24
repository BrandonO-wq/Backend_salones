-- ============================================================
-- Tabla: refresh_tokens
-- Descripción: Almacena los refresh tokens activos y revocados.
--              El token real no se guarda: se guarda su hash SHA-256
--              para no exponer el token en texto plano en la BD.
-- ============================================================

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  TEXT        NOT NULL,              -- hash SHA-256 del refresh token
    expires_at  TIMESTAMPTZ NOT NULL,              -- fecha de expiración del token
    revoked_at  TIMESTAMPTZ,                       -- null = activo, fecha = revocado
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Índice para búsquedas por usuario (listar/revocar tokens de un usuario)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id   ON refresh_tokens (user_id);

-- Índice para búsqueda rápida por hash al validar el token recibido
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
