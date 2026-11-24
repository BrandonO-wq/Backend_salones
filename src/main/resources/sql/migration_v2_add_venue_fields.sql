-- Migración para añadir nuevos campos al módulo de Venues (Salones de Eventos)

ALTER TABLE venues 
    -- Capacidades
    ADD COLUMN IF NOT EXISTS min_capacity INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_capacity INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS banquet_capacity INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS theater_capacity INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cocktail_capacity INTEGER DEFAULT 0,
    
    -- Infraestructura
    ADD COLUMN IF NOT EXISTS bathrooms_count INTEGER DEFAULT 0,
    
    -- Características Técnicas (Arrays de IDs de parámetros)
    ADD COLUMN IF NOT EXISTS id_amenities INTEGER[] DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS id_furniture INTEGER[] DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS id_climate INTEGER[] DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS id_audio_video INTEGER[] DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS id_lighting INTEGER[] DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS id_facilities INTEGER[] DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS id_accessibility INTEGER[] DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS id_services INTEGER[] DEFAULT '{}',
    
    -- Reglas de Precio y Horario

    -- Políticas
    ADD COLUMN IF NOT EXISTS min_advance_days INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cancellation_policy TEXT;

-- Comentario para recordar la actualización de los registros existentes si fuera necesario
COMMENT ON TABLE venues IS 'Tabla de locales deportivos y salones de eventos con capacidades y características técnicas.';
