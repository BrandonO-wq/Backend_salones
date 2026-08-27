# API Endpoints — Proyecto CanchaGo Completo

> Documentación de todos los endpoints expuestos en los controladores. Incluye métodos, requests, responses y códigos de error.

---

## 1. Módulo: Autenticación (`/auth`)

### 1.1 `POST /auth/register`
**Auth:** Público  
**Descripción:** Registra un nuevo usuario con rol CLIENT en estado PENDIENTE y envía un código OTP (4 dígitos) al correo.

**Request Body:**
```json
{
  "email": "juan.perez@example.com",
  "password": "Password123!",
  "name": "Juan Pérez",
  "phone": "+51987654321"
}
```

**Response 201:**
```json
{
  "userId": "11223344-5566-7788-9900-aabbccddeeff"
}
```

**Response 400 (Datos Inválidos):**
```json
{
  "code": "BAD_REQUEST",
  "message": "Validation failed",
  "details": ["password: debe tener al menos 8 caracteres"]
}
```

**Response 409 (Email Registrado):**
```json
{
  "code": "CONFLICT",
  "message": "El email ya está registrado y activo"
}
```

---

### 1.2 `POST /auth/verify-account`
**Auth:** Público  
**Descripción:** Valida el código OTP. Si es correcto, activa la cuenta y retorna tokens para auto-login.

**Request Body:**
```json
{
  "userId": "11223344-5566-7788-9900-aabbccddeeff",
  "otpCode": "1234"
}
```

**Response 200:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI...",
  "refreshToken": "eyJhbGciOiJIUzI...",
  "expiresIn": 3600
}
```

**Response 400 (OTP Incorrecto):**
```json
{
  "code": "BAD_REQUEST",
  "message": "Código OTP incorrecto o expirado"
}
```

**Response 404 (No Encontrado):**
```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Usuario no encontrado"
}
```

---

### 1.3 `POST /auth/resend-otp`
**Auth:** Público  
**Descripción:** Reenvía un nuevo OTP al email del usuario pendiente (Aplica cooldown de 2 min).

**Request Body:**
```json
{
  "userId": "11223344-5566-7788-9900-aabbccddeeff"
}
```

**Response 200:**
```json
{
  "message": "OTP reenviado exitosamente"
}
```

**Response 429 (Cooldown):**
```json
{
  "code": "TOO_MANY_REQUESTS",
  "message": "Cooldown activo: debes esperar antes de reenviar el OTP"
}
```

---

### 1.4 `POST /auth/forgot-password`
**Auth:** Público  
**Descripción:** Envía un OTP de recuperación genérico por razones de seguridad.

**Request Body:**
```json
{
  "email": "juan.perez@example.com"
}
```

**Response 200:**
```json
{
  "message": "Si el email existe, se ha enviado un código de recuperación."
}
```

---

### 1.5 `POST /auth/verify-recovery-otp`
**Auth:** Público  
**Descripción:** Verifica el OTP de recuperación y retorna un token de un solo uso (`resetToken`).

**Request Body:**
```json
{
  "userId": "11223344-5566-7788-9900-aabbccddeeff",
  "otpCode": "1234"
}
```

**Response 200:**
```json
{
  "resetToken": "0d2cceb2-7634-4b52-9f3b-851bfca302e1"
}
```

**Response 400:**
```json
{
  "code": "BAD_REQUEST",
  "message": "OTP incorrecto o expirado"
}
```

---

### 1.6 `POST /auth/reset-password`
**Auth:** Público  
**Descripción:** Efectúa el reseteo de la contraseña usando el `resetToken`.

**Request Body:**
```json
{
  "resetToken": "0d2cceb2-7634-4b52-9f3b-851bfca302e1",
  "newPassword": "NewPassword456!"
}
```

**Response 200:**
```json
{
  "message": "Contraseña actualizada correctamente"
}
```

---

### 1.7 `POST /auth/login`
**Auth:** Público  
**Descripción:** Autentica a usuarios ACTIVOS con rol CLIENT.

**Request Body:**
```json
{
  "email": "juan.perez@example.com",
  "password": "Password123!"
}
```

**Response 200:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI...",
  "refreshToken": "eyJhbGciOiJIUzI...",
  "expiresIn": 3600
}
```

**Response 401 (Incorrectas):**
```json
{
  "code": "UNAUTHORIZED",
  "message": "Credenciales incorrectas o usuario inactivo"
}
```

---

### 1.8 `POST /auth/admin/login`
**Auth:** Público  
**Descripción:** Exclusivo para usuarios ACTIVOS con rol ADMIN.

**Request Body:** (Igual a LoginRequest)

**Response 403 (No es Admin):**
```json
{
  "code": "FORBIDDEN",
  "message": "El usuario no tiene los permisos requeridos (ROL ADMIN)"
}
```

---

### 1.9 `POST /auth/refresh`
**Auth:** Público  
**Descripción:** Rotación de tokens. Revoca el refreshToken usado y envía un par fresco.

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI..."
}
```

**Response 200:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI...",
  "refreshToken": "eyJhbGciOiJIUzI...",
  "expiresIn": 3600
}
```

**Response 409 (Revocado/Inválido):**
```json
{
  "code": "CONFLICT",
  "message": "Refresh token inválido, revocado o expirado"
}
```

---

### 1.10 `POST /auth/logout`
**Auth:** 🔒 JWT Bearer  
**Descripción:** Revoca la sesión actual, enviando tokens JWT al banlist (Redis).

**Response 200:**
```json
{
  "message": "Sesión cerrada exitosamente"
}
```

---

### 1.11 `POST /auth/updatefbtoken`
**Auth:** 🔒 JWT Bearer  
**Descripción:** Vincula o actualiza el token de FCM Firebase para Notificaciones Push.

**Request Body:**
```json
{
  "fbToken": "fP9_ABC123xyz_token_for_push_notifications"
}
```

**Response 200:**
```json
{
  "message": "Token de Firebase actualizado"
}
```

---

### 1.12 `GET /auth/me`
**Auth:** 🔒 JWT Bearer  
**Descripción:** Retorna el perfil completo (id, email, fullName, roles) asociado al JWT.

**Response 200:**
```json
{
  "id": "11223344-5566-7788-9900-aabbccddeeff",
  "name": "Juan Pérez",
  "email": "juan.perez@example.com",
  "phone": "+51987654321",
  "roles": ["ROLE_CLIENT"]
}
```

---

## 2. Módulo: Perfiles (`/users`)

### 2.1 `PATCH /users/profile`
**Auth:** 🔒 JWT Bearer  
**Descripción:** Modifica el perfil del usuario autenticado.

**Request Body:**
```json
{
  "name": "Juan Pérez Actualizado",
  "phone": "+51987654322"
}
```

**Response 200:**
```json
{
  "message": "Perfil actualizado correctamente"
}
```

---

## 3. Módulo: Venues / Canchas (`/venues`)

### 3.1 `GET /parameters/{parentId}`
**Auth:** Público (Cache Redis 12h)  
**Categorías padre:** 1=Deportes, 2=Superficies, 3=Comodidades, 4=Bancos, 5=Billeteras.

**Response 200:**
```json
[
  {
    "idParam": 101,
    "idParamParent": 1,
    "name": "Fútbol",
    "description": "Fútbol",
    "icon": "sports_soccer",
    "iconType": 1,
    "status": 1
  }
]
```

---

### 3.2 `GET /ubigeos`
**Auth:** Público (Cache Redis 24h)  
**Descripción:** Retorna árbol de 3 niveles del INEI.

**Response 200:**
```json
{
  "departamentos": [
    {
      "code": "15",
      "name": "Lima",
      "provincias": [
        {
          "code": "1501",
          "name": "Lima",
          "distritos": [
            { "code": "150101", "name": "Lima" }
          ]
        }
      ]
    }
  ]
}
```

---

### 3.3 `POST /venues`
**Auth:** 🔒 JWT Bearer  
**Descripción:** Crea una sede deportiva garantizando que siempre reciba dos `scheduleType` (1 y 2, diurno y nocturno).

**Request Body:**
```json
{
  "name": "Cancha Los Campeones",
  "address": "Av. La Marina 2850, San Miguel",
  "latitude": -12.0764,
  "longitude": -77.0890,
  "ubigeo": "150136",
  "description": "Cancha de fútbol con césped sintético de última generación",
  "dimensions": "40m x 20m",
  "isIlluminated": true,
  "idSports": [101, 103],
  "idSurfaces": [202],
  "idComforts": [301, 303, 305],
  "schedules": [
    {
      "scheduleType": 1,
      "startTime": "06:00",
      "endTime": "18:00",
      "price": 80.00,
      "days": [1, 2, 3, 4, 5],
      "status": 1
    },
    {
      "scheduleType": 2,
      "startTime": "18:00",
      "endTime": "23:00",
      "price": 120.00,
      "days": [1, 2, 3, 4, 5, 6, 7],
      "status": 1
    }
  ],
  "payBanks": [
    {
      "idBank": 401,
      "nombreTitular": "Juan Pérez García",
      "numeroCuenta": "19112345678901",
      "cci": "00219112345678901234"
    }
  ],
  "payWallets": [
    {
      "idDigitalWallet": 501,
      "nombreTitular": "Juan Pérez García",
      "numeroCelular": "987654321"
    }
  ]
}
```

**Response 201:**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "hostUserId": "11223344-5566-7788-9900-aabbccddeeff",
  "name": "Cancha Los Campeones",
  "address": "Av. La Marina 2850, San Miguel",
  "latitude": -12.0764,
  "longitude": -77.0890,
  "ubigeo": "150136",
  "description": "Cancha de fútbol con césped sintético",
  "dimensions": "40m x 20m",
  "isIlluminated": true,
  "idSports": [101, 103],
  "idSurfaces": [202],
  "idComforts": [301, 303, 305],
  "status": 1,
  "createdAt": "2026-03-26T15:00:00Z",
  "schedules": [
    {
      "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "scheduleType": 1,
      "startTime": "06:00",
      "endTime": "18:00",
      "price": 80.00,
      "days": [1, 2, 3, 4, 5],
      "status": 1
    },
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "scheduleType": 2,
      "startTime": "18:00",
      "endTime": "23:00",
      "price": 120.00,
      "days": [1, 2, 3, 4, 5, 6, 7],
      "status": 1
    }
  ],
  "payBanks": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "idBank": 401,
      "nombreTitular": "Juan Pérez García",
      "numeroCuenta": "19112345678901",
      "cci": "00219112345678901234"
    }
  ],
  "payWallets": [
    {
      "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "idDigitalWallet": 501,
      "nombreTitular": "Juan Pérez García",
      "numeroCelular": "987654321"
    }
  ],
  "photos": [],
  "avgRating": 0.0,
  "reviewCount": 0
}
```

**Response 400 (Bad Request):**
```json
{
  "code": "BAD_REQUEST",
  "message": "Faltan horarios. Se requiere scheduleType 1 y 2."
}
```

---

### 3.4 `GET /venues/my`
**Auth:** 🔒 JWT Bearer  
**Descripción:** Retorna los venues activos del host autenticado.

**Response 200:**
```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "name": "Cancha Los Campeones",
    "address": "Av. Universitaria 1234, Lima",
    "latitude": -12.0764,
    "longitude": -77.0890,
    "ubigeo": "150136",
    "illuminated": true,
    "sports": [101, 103],
    "surfaces": [202],
    "avgRating": 4.8,
    "reviewCount": 12,
    "photoUrl": "e4d909c2-90d0-4c3f-b1a8-427fc7b1e3af"
  }
]
```

---

### 3.5 `GET /venues/{id}`
**Auth:** Público  
**Descripción:** Detalle completo de un venue con sus horarios, métodos de pago, fotos y rating.

**Response 200:**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "hostUserId": "11223344-5566-7788-9900-aabbccddeeff",
  "name": "Cancha Los Campeones",
  "address": "Av. La Marina 2850, San Miguel",
  "latitude": -12.0764,
  "longitude": -77.0890,
  "ubigeo": "150136",
  "description": "Cancha de fútbol con césped sintético de última generación",
  "dimensions": "40m x 20m",
  "isIlluminated": true,
  "idSports": [101, 103],
  "idSurfaces": [202],
  "idComforts": [301, 303, 305],
  "status": 1,
  "createdAt": "2026-03-26T15:00:00Z",
  "schedules": [
    {
      "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "scheduleType": 1,
      "startTime": "06:00",
      "endTime": "18:00",
      "price": 80.00,
      "days": [1, 2, 3, 4, 5],
      "status": 1
    },
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "scheduleType": 2,
      "startTime": "18:00",
      "endTime": "23:00",
      "price": 120.00,
      "days": [1, 2, 3, 4, 5, 6, 7],
      "status": 1
    }
  ],
  "payBanks": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "idBank": 401,
      "nombreTitular": "Juan Pérez García",
      "numeroCuenta": "19112345678901",
      "cci": "00219112345678901234"
    }
  ],
  "payWallets": [
    {
      "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "idDigitalWallet": 501,
      "nombreTitular": "Juan Pérez García",
      "numeroCelular": "987654321"
    }
  ],
  "photos": [
    {
      "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
      "url": "019504a2-3b1c-7d4e.jpg",
      "createdAt": "2026-03-26T16:00:00Z"
    }
  ],
  "avgRating": 4.5,
  "reviewCount": 12
}
```

**Response 404 (No existe):**
```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Venue no encontrado con id: a1b2c3d4-..."
}
```

---

### 3.6 `PATCH /venues/{id}`
**Auth:** 🔒 JWT (Propietario)  
**Descripción:** Actualiza un venue parcialmente evaluando solo los valores no-nulos.

**Request Body:**
```json
{
  "name": "Cancha Los Campeones PREMIUM",
  "isIlluminated": true,
  "idComforts": [301, 303, 305, 307]
}
```

**Response 200:** (Retorna todo el Venue modificado, igual al objeto base de GET /venues/{id}).

**Response 403 (No es propietario):**
```json
{
  "code": "FORBIDDEN",
  "message": "El usuario no es propietario de este venue"
}
```

---

### 3.7 `DELETE /venues/{id}`
**Auth:** 🔒 JWT (Propietario)  
**Descripción:** Soft-delete (status = 0) de un venue. Solo el host creador puede hacerlo.

**Response 200:**
```json
{
  "message": "Venue desactivado correctamente"
}
```

---

### 3.8 `POST /venues/in-bounds`
**Auth:** Público  
**Descripción:** Búsqueda rápida de catálogo en mapa (PostGIS).

**Request Body:**
```json
{
  "latitude": -12.0764,
  "longitude": -77.0890,
  "radiusKm": 15
}
```

**Response 200:**
```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "name": "Cancha Los Campeones",
    "address": "Av. La Marina 2850",
    "latitude": -12.0764,
    "longitude": -77.0890,
    "ubigeo": "150136",
    "illuminated": true,
    "sports": [101, 103],
    "surfaces": [202],
    "avgRating": 4.5,
    "reviewCount": 12,
    "photoUrl": "019504a2-3b1c-7d4e.jpg"
  }
]
```

---

### 3.9 `POST /venues/search`
**Auth:** Público  
**Descripción:** Búsqueda avanzada de venues construyendo where dinámicos en SQL y ordenados por relevancia.

**Request Body:**
```json
{
  "sportId": 101,
  "surfaceIds": [202, 203],
  "isIlluminated": true,
  "keyword": "campeones",
  "latitude": -12.0464,
  "longitude": -77.0428,
  "radiusKm": 10
}
```

**Response 200:** (Array de listado de venues idéntico a `/in-bounds`).

---

### 3.10 `POST /venues/{id}/photos`
**Auth:** 🔒 JWT (Propietario)  
**Content-Type:** `multipart/form-data`  
**Descripción:** Sube fotos JPG/PNG/WEBP (máx. 5MB c/u). Las comprime y retiene hasta 5 imágenes finales.
- **Files Param:** `files` (Array Binario)

**Response 201:**
```json
{
  "message": "3 foto(s) subida(s) correctamente"
}
```

**Response 400 (Error Cantidad):**
```json
{
  "code": "BAD_REQUEST",
  "message": "Máximo 5 fotos. Actuales: 3, nuevas: 4"
}
```

---

### 3.11 `GET /venues/photos/{photoId}`
**Auth:** Público  
**Descripción:** Retorna el Image Stream en crudo directo de almacenamiento local reconstruyendo la metadata y extension original.

**Response 404 (No existe File/UUID):**
```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "VenuePhoto no encontrada en el sistema"
}
```

---

### 3.12 `DELETE /venues/photos/{photoId}`
**Auth:** 🔒 JWT (Propietario)  
**Descripción:** Elimina del servidor físico y de la base de datos la foto solicitada, siempre y cuando la cancha pertenezca al usuario JWT y la foto exista.

**Response 200:**
```json
{
  "message": "Foto eliminada correctamente"
}
```

**Response 403 (No Propietario):**
```json
{
  "code": "FORBIDDEN",
  "message": "No tiene permisos para modificar este venue"
}
```

---


## 4. Módulo: Reseñas (`/venues/{venueId}/reviews`)

### 4.1 `POST /venues/{venueId}/reviews`
**Auth:** 🔒 JWT Bearer  
**Descripción:** El usuario evalúa el venue de 1 a 5. Se bloquea si ya la evaluó.

**Request Body:**
```json
{
  "points": 4,
  "description": "Excelente cancha, muy bien cuidada y con buenas instalaciones."
}
```

**Response 201:**
```json
{
  "id": "e4d909c2-90d0-4c3f-b1a8-427fc7b1e3af",
  "venueId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "userId": "11223344-5566-7788-9900-aabbccddeeff",
  "points": 4,
  "description": "Excelente cancha, muy bien cuidada y con buenas instalaciones.",
  "createdAt": "2026-03-26T16:30:00Z"
}
```

**Response 409 (Duplicado):**
```json
{
  "code": "CONFLICT",
  "message": "Ya has dejado una reseña para este venue. Usa PUT para actualizarla."
}
```

---

### 4.2 `PUT /venues/{venueId}/reviews`
**Auth:** 🔒 JWT Bearer  
**Descripción:** Actualiza tu propia reseña en la sede elegida enviando un payload similar al `POST`.

**Request Body:**
```json
{
  "points": 5,
  "description": "Actualización: después de varias visitas, la cancha sigue impecable."
}
```

**Response 200:** (Muestra el mismo DTO `ReviewResponse` con `points` de 5 y nueva descripción).

---

### 4.3 `DELETE /venues/{venueId}/reviews/{bookingId}`
**Auth:** 🔒 JWT Bearer  
**Descripción:** Revoca irreversiblemente tu reseña. El `bookingId` se exige para validar de qué reserva proviene esta reseña.

**Response 200:**
```json
{
  "message": "Reseña eliminada correctamente"
}
```

---

### 4.4 `GET /venues/{venueId}/reviews`
**Auth:** Público  
**Descripción:** Lista las reseñas publicadas ordenadas por el más reciente (`createdAt DESC`).

**Response 200:**
```json
[
  {
    "id": "e4d909c2-90d0-4c3f-b1a8-427fc7b1e3af",
    "venueId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "userId": "11223344-5566-7788-9900-aabbccddeeff",
    "points": 5,
    "description": "Excelente cancha...",
    "createdAt": "2026-03-26T16:30:00Z"
  }
]
```

---

### 4.5 `GET /venues/{venueId}/reviews/summary`
**Auth:** Público  
**Descripción:** Calcula el promedio aritmético en DB y recuenta total de reseñas activas.

**Response 200:**
```json
{
  "avgRating": 4.2,
  "reviewCount": 15
}
```

---

## 5. Módulo: Reservas (`/bookings`)

### 5.1 `GET /bookings/slots`
**Auth:** Público  
**Descripción:** Obtener slots disponibles por fecha y sede.

**Query Params:** `?venueId=a1b2c3d4-e5f6-7890-abcd-ef1234567890&date=2026-05-18`

**Response 200:**
```json
{
  "venueId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "date": "2026-05-18",
  "slots": [
    {
      "time": "18:00 - 19:00",
      "price": 120.00,
      "isAvailable": true,
      "status": "AVAILABLE"
    },
    {
      "time": "19:00 - 20:00",
      "price": 120.00,
      "isAvailable": false,
      "status": "HELD"
    }
  ]
}
```

---

### 5.2 `POST /bookings/hold`
**Auth:** 🔒 JWT Bearer (`ROLE_CLIENT`)  
**Descripción:** Bloquear slots temporalmente (10 min) para realizar el pago. Este bloqueo expira automáticamente si no hay pago confirmado (Timer).

**Request Body:**
```json
{
  "venueId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "scheduleIds": [
    "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "7c9e6679-7425-40de-944b-e07fc1f90ae7"
  ]
}
```

**Response 200:**
```json
{
  "holdId": "eb2564b1-6a3f-4e00-a23d-ff960570b6d2",
  "expiresAt": "2026-04-18T10:15:00Z"
}
```

**Response 409 (Conflicto - Slot Ocupado o Bloqueado):**
```json
{
  "code": "CONFLICT",
  "message": "Uno o más horarios no están disponibles"
}
```

---

### 5.3 `POST /bookings/hold/{holdId}/extend`
**Auth:** 🔒 JWT Bearer (`ROLE_CLIENT`)  
**Descripción:** Extender bloqueo (+5 min) de una reserva en proceso. Sólo el dueño del hold puede extenderlo.

**Response 200:**
```json
{
  "holdId": "eb2564b1-6a3f-4e00-a23d-ff960570b6d2",
  "expiresAt": "2026-04-18T10:20:00Z"
}
```

**Response 403 (No es propietario):**
```json
{
  "code": "FORBIDDEN",
  "message": "No tiene permisos para extender este bloqueo"
}
```

---

### 5.4 `POST /bookings` (Consumes Multipart Form Data)
**Auth:** 🔒 JWT Bearer (`ROLE_CLIENT`)  
**Descripción:** Confirmar reserva cliente enviando el voucher de pago (foto).

**Request Form Data:**
- `holdId`: `"eb2564b1-6a3f-4e00-a23d-ff960570b6d2"`
- `idPaymentMethod`: `401`
- `description`: `"Pago por 2 horas"`
- `voucher`: (Archivo binario JPG/PNG Max 5MB)
- `sports_id`: `[101, 103]` (Array de enteros enviado como parte de la data del registro o separados por comas según framework)

**Response 200:**
```json
{
  "bookingId": "c8e9b04f-7bde-4df4-8dcf-caafb9d1469e",
  "status": 1, 
  "totalAmount": 240.00
}
```

**Response 404 (Hold No Existe / Expiró):**
```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "El bloqueo ingresado no existe o ha expirado"
}
```

---

### 5.5 `GET /bookings/{id}`
**Auth:** 🔒 JWT Bearer (`ROLE_CLIENT`)  
**Descripción:** Obtener detalle de reserva de un cliente por su ID.

**Response 200:**
```json
{
  "id": "c8e9b04f-7bde-4df4-8dcf-caafb9d1469e",
  "venueId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "clientId": "11223344-5566-7788-9900-aabbccddeeff",
  "totalAmount": 240.00,
  "status": 1,
  "idPaymentMethod": 401,
  "description": "Pago por 2 horas"
}
```

---

### 5.6 `DELETE /bookings/hold/{holdId}`
**Auth:** 🔒 JWT Bearer (`ROLE_CLIENT`)  
**Descripción:** Liberar bloqueo manualmente sin finalizar compra.

**Response 200:**
```json
{
  "message": "Bloqueo liberado con éxito"
}
```

---

### 5.7 `POST /bookings/host/{id}/confirm`
**Auth:** 🔒 JWT Bearer (`ROLE_CLIENT`)  
**Descripción:** Host confirma reserva (Status=3) realizada por cliente. Aplica lógica anti-leaks verificando pertenencia.

**Response 200:**
```json
{
  "message": "Reserva confirmada exitosamente por el Host"
}
```

**Response 403 (No es propietario de la cancha):**
```json
{
  "code": "FORBIDDEN",
  "message": "No tiene permisos para confirmar reservas de esta cancha"
}
```

---

### 5.8 `POST /bookings/host/{id}/reject`
**Auth:** 🔒 JWT Bearer (`ROLE_CLIENT`)  
**Descripción:** Host rechaza reserva (Status=2).

**Response 200:**
```json
{
  "message": "Reserva rechazada exitosamente por el Host"
}
```

---

### 5.9 `GET /bookings/host`
**Auth:** 🔒 JWT Bearer (`ROLE_CLIENT`)  
**Descripción:** Obtener la lista completa de reservas del anfitrión.

**Response 200:**
```json
[
  {
    "id": "c8e9b04f-7bde-4df4-8dcf-caafb9d1469e",
    "venueName": "Cancha Los Campeones",
    "code": "BK-10293",
    "clientName": "Juan Pérez",
    "clientPhone": "+51987654321",
    "timeSlot": "18:00 - 20:00",
    "date": "2026-05-18",
    "amount": 240.00,
    "commissionRate": 10.00,
    "commissionAmount": 24.00,
    "netAmount": 216.00,
    "status": 1,
    "paid": true,
    "isExpired": false,
    "voucherUrl": "https://...",
    "message": "Reserva confirmada",
    "confirmedAt": "2026-05-10T14:30:00Z"
  }
]
```

---

### 5.10 `GET /bookings/host/pending-count`
**Auth:** 🔒 JWT Bearer (`ROLE_CLIENT`)  
**Descripción:** Obtener cantidad de reservas pendientes de confirmación.

**Response 200:**
```json
{
  "count": 2
}
```

---

### 5.11 `GET /bookings/client`
**Auth:** 🔒 JWT Bearer (`ROLE_CLIENT`)  
**Descripción:** Obtener la lista completa del historial de reservas hechas por el cliente.

**Response 200:**
```json
[
  {
    "id": "c8e9b04f-7bde-4df4-8dcf-caafb9d1469e",
    "venueId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "venueName": "Cancha Los Campeones",
    "venueImageUrl": "019504a2-3b1c-7d4e.jpg",
    "code": "BK-240501",
    "date": "2026-05-18",
    "timeSlot": "18:00 - 20:00",
    "address": "Av. La Marina 2850",
    "latitude": -12.0764,
    "longitude": -77.0890,
    "ubigeo": "150136",
    "contactNumber": "987654321",
    "price": 240.00,
    "status": 1,
    "sportName": "Fútbol, Vóley",
    "venueSportIds": [101, 103],
    "isExpired": false,
    "userReviewPoints": null,
    "userReviewComment": null
  }
]
```

---

### 5.12 `POST /bookings/validate-slots`
**Auth:** 🔒 JWT Bearer (`ROLE_CLIENT`)  
**Descripción:** Valida la disponibilidad de uno o varios horarios específicos sin realizar un bloqueo permanente. Mismo DTO de Hold.

**Request Body:**
```json
{
  "venueId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "date": "2026-05-18",
  "slots": [
    {
      "scheduleType": 1,
      "startTime": "18:00"
    }
  ]
}
```

**Response 200:**
```json
{
  "available": [
    {
      "scheduleType": 1,
      "startTime": "18:00"
    }
  ],
  "unavailable": []
}
```

---

## 6. Módulo: Notificaciones (`/notifications`)

### 6.1 `GET /notifications`
**Auth:** 🔒 JWT Bearer
**Descripción:** Obtener la bandeja paginada de notificaciones para el usuario autenticado.

**Query Params:** `?page=0&size=20`

**Response 200:**
```json
{
  "content": [
    {
      "id": "18f5d023-e4d6-72aa-a3fb-0cf2b4ebd420",
      "type": "NEW_BOOKING",
      "title": "Nueva Reserva",
      "message": "Tienes una solicitud de reserva.",
      "read": false,
      "referenceId": "c8e9b04f-7bde-4df4-8dcf-caafb9d1469e",
      "createdAt": "2026-04-10T15:00:00Z"
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 6.2 `GET /notifications/unread-count`
**Auth:** 🔒 JWT Bearer
**Descripción:** Obtener el contador en tiempo real de notificaciones no leídas.

**Response 200:**
```json
{
  "count": 5
}
```

---

### 6.3 `PATCH /notifications/{id}/read`
**Auth:** 🔒 JWT Bearer
**Descripción:** Marcar una notificación específica como leída.

**Response 200:**
```json
{
  "message": "Marked as read"
}
```

---

### 6.4 `PATCH /notifications/read-all`
**Auth:** 🔒 JWT Bearer
**Descripción:** Marcar todas las notificaciones del usuario como leídas en lote.

**Response 200:**
```json
{
  "message": "All marked as read"
}
```

---

## 7. Resumen de Enrutamento API

| Endpoint | Método | Auth |
|---|---|---|
| `/auth/register` | POST | 🌐 Público |
| `/auth/verify-account` | POST | 🌐 Público |
| `/auth/resend-otp` | POST | 🌐 Público |
| `/auth/forgot-password` | POST | 🌐 Público |
| `/auth/verify-recovery-otp` | POST | 🌐 Público |
| `/auth/reset-password` | POST | 🌐 Público |
| `/auth/login` | POST | 🌐 Público |
| `/auth/admin/login` | POST | 🌐 Público |
| `/auth/refresh` | POST | 🌐 Público |
| `/auth/logout` | POST | 🔒 JWT Bearer |
| `/auth/updatefbtoken` | POST | 🔒 JWT Bearer |
| `/auth/me` | GET | 🔒 JWT Bearer |
| `/users/profile` | PATCH | 🔒 JWT Bearer |
| `/parameters/{parentId}` | GET | 🌐 Público |
| `/ubigeos` | GET | 🌐 Público |
| `/venues` | POST | 🔒 JWT Bearer |
| `/venues/my` | GET | 🔒 JWT Bearer |
| `/venues/{id}` | GET | 🌐 Público |
| `/venues/{id}` | PATCH | 🔒 JWT + Owner |
| `/venues/{id}` | DELETE | 🔒 JWT + Owner |
| `/venues/in-bounds` | POST | 🌐 Público |
| `/venues/search` | POST | 🌐 Público |
| `/venues/{id}/photos` | POST | 🔒 JWT + Owner |
| `/venues/photos/{photoId}` | GET | 🌐 Público |
| `/venues/photos/{photoId}` | DELETE | 🔒 JWT + Owner |
| `/venues/{venueId}/reviews` | POST | 🔒 JWT Bearer |
| `/venues/{venueId}/reviews` | PUT | 🔒 JWT Bearer |
| `/venues/{venueId}/reviews/{bookingId}` | DELETE | 🔒 JWT Bearer |
| `/venues/{venueId}/reviews` | GET | 🌐 Público |
| `/venues/{venueId}/reviews/summary` | GET | 🌐 Público |
| `/notifications` | GET | 🔒 JWT Bearer |
| `/notifications/unread-count` | GET | 🔒 JWT Bearer |
| `/notifications/{id}/read` | PATCH | 🔒 JWT Bearer |
| `/notifications/read-all` | PATCH | 🔒 JWT Bearer |
| `/bookings/slots` | GET | 🌐 Público |
| `/bookings/hold` | POST | 🔒 JWT Bearer |
| `/bookings` | POST | 🔒 JWT Bearer |
| `/bookings/{id}` | GET | 🔒 JWT Bearer |
| `/bookings/hold/{holdId}` | DELETE | 🔒 JWT Bearer |
| `/bookings/validate-slots` | POST | 🔒 JWT Bearer |
| `/bookings/host/{id}/confirm` | POST | 🔒 JWT Bearer |
| `/bookings/host/{id}/reject` | POST | 🔒 JWT Bearer |
| `/bookings/host` | GET | 🔒 JWT Bearer |
| `/bookings/host/pending-count` | GET | 🔒 JWT Bearer |
| `/bookings/client` | GET | 🔒 JWT Bearer |

---

## 8. Diccionario de Datos (Enums)

Para evitar el uso de *magic numbers* y garantizar integridad de datos, la API se rige mediante los siguientes enumeradores nativos del backend:

### BookingStatus (Estado de la Reserva)
| ID | Enum | Descripción |
|---|---|---|
| `1` | **PENDING** | Reserva iniciada, pendiente de confirmación manual por el Host o automatizada. |
| `2` | **REJECTED** | Reserva rechazada explícitamente por el Host. |
| `3` | **CONFIRMED** | Reserva confirmada (aprobada y vigente). |
| `4` | **COMPLETED** | Reserva cuya hora de fin ya transcurrió. |

### VenueStatus (Estado Lógico de la Cancha)
| ID | Enum | Descripción |
|---|---|---|
| `0` | **INACTIVE** | Cancha dada de baja (Soft Delete) o sin publicar. |
| `1` | **ACTIVE** | Cancha operativa y visible en el catálogo de búsquedas. |

### ScheduleType (Tipos de Horario)
| ID | Enum | Descripción |
|---|---|---|
| `1` | **DAY** | Horario Diurno (Típicamente precios base sin recargo de luz). |
| `2` | **NIGHT** | Horario Nocturno (Suelen aplicar recargos por iluminación). |

### NotificationType (Eventos Push)
Identificadores para el gestor de FCM y bandeja de entrada.
- `NEW_BOOKING`: Notificación estándar enviada al host apenas se efectúa un pago.
- `HOST_PENDING_REMINDER`: Recordatorio enviado al host para que no olvide revisar una reserva pendiente.
- `SYSTEM_AUTO_CONFIRMED`: Notificación de que el backend auto-confirmó una reserva desatendida.
- `CLIENT_UPCOMING_REMINDER`: Recordatorio al cliente avisando que su evento empieza pronto (ej. 1 hora).

---

## 9. Tareas Automatizadas y en Segundo Plano (Schedulers)

El backend de CanchaGo ejecuta Jobs periódicamente (`@Scheduled`) de forma asíncrona para automatizar el ciclo de vida de la base de datos sin requerir requests HTTP manuales.

### 9.1 `BookingStatusScheduler`
- **Frecuencia:** `app.scheduler.booking.interval` (Por defecto cada minuto).
- **Responsabilidad:** Barre la tabla de reservas confirmadas (`status = 3`). Si la fecha y hora de fin de los horarios (`BookingDetailEntity`) ya superó a la hora actual (`currentDateTime`), migra automáticamente la reserva al estado **COMPLETED** (`4`).

### 9.2 `BookingNotificationScheduler`
- **Frecuencia:** `app.scheduler.push.interval` (Por defecto cada minuto).
- **Responsabilidades (Flujo de vida del PENDING y CONFIRMED):**
  1. **Recordatorios al Host:** Busca reservas `PENDING` de autorización. Si transcurrió el tiempo de espera configurado (`app.scheduler.push.host-reminder-minutes`), dispara un `HOST_PENDING_REMINDER` para apurar al anfitrión.
  2. **Auto-Confirmación del Sistema:** Si siguen transcurriendo los minutos hasta alcanzar el límite configurado (`app.scheduler.push.host-autoconfirm-minutes`), invoca programáticamente al servicio y aprueba la reserva (pasa a `CONFIRMED(3)`), evitando así que el jugador pierda su oportunidad. Emite la notificación `SYSTEM_AUTO_CONFIRMED`.
  3. **Avisos al Cliente (Upcoming):** Escanea las reservas `CONFIRMED` que ocurren en el día actual (Today). Si el tiempo de inicio real del alquiler se acerca a la meta pre-configurada (`app.scheduler.push.client-reminder-minutes`), se le notifica por push alertando al jugador que su partido está a punto de iniciar (`CLIENT_UPCOMING_REMINDER`).
