# POIs Manager

App web para viajeros (React + Vite + Tailwind) sobre la API de puntos de interés (Spring Boot + PostgreSQL), todo en Docker.

## Arranque

```bash
./setup.sh
```

Requisito único: Docker con Compose v2. El script genera `.env` con contraseñas aleatorias (solo la primera vez), construye las imágenes, espera a que todo esté sano e imprime la URL y las credenciales del administrador.

## Qué incluye

| Contenedor | Rol |
|---|---|
| `postgres` | Base de datos (volumen `pgdata`) |
| `backend` | API Spring Boot (no expuesta; solo vía nginx) |
| `frontend` | nginx: sirve la SPA y hace proxy de `/api/*` al backend |

- **Explorar** y **planear ruta**: gratis y sin registro (60 peticiones/min por IP).
- **Registro**: solo para obtener API keys (`tr_…`). Se guardan hasheadas (SHA-256), se muestran una sola vez, tienen cuota diaria (1,000/día por defecto), máximo 3 activas por cuenta, historial de uso de 7 días y se pueden revocar.
- **Admin** (`/admin`): métricas, revocar/reactivar keys, cambiar su cuota, deshabilitar usuarios, alta y baja de lugares.
- Al primer arranque se cargan 44 lugares de ejemplo en México.

Uso de la API: ver `/docs` en la app. Cabecera: `X-API-Key: tr_…` (o `Authorization: Bearer tr_…`).

## Configuración

Todo vive en `.env` (lo crea `setup.sh`): puerto, credenciales, `ANON_REQUESTS_PER_MINUTE`, `API_KEY_DAILY_LIMIT`, `API_KEY_MAX_PER_USER`. Tras editarlo, vuelve a ejecutar `./setup.sh`.

La contraseña del admin se sincroniza desde `.env` en cada arranque del backend.

## Desarrollo local

```bash
cd backend && DB_URL=jdbc:postgresql://localhost:5432/pois DB_USER=pois DB_PASS=… JWT_SECRET=$(openssl rand -hex 32) mvn spring-boot:run
cd frontend && npm install && npm run dev   # proxy /api -> localhost:8082
```
