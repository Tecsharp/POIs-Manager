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

- **Explorar** y **planear ruta**: gratis y sin registro (60 peticiones/min por IP). Las rutas siguen la carretera real (OSRM), no líneas rectas.
- **Registro**: para obtener API keys (`tr_…`) y para dar de alta lugares (`/agregar`) y votarlos como útiles o no. Las keys se guardan hasheadas (SHA-256), se muestran una sola vez, tienen cuota diaria (1,000/día por defecto), máximo 3 activas por cuenta, historial de uso de 7 días y se pueden revocar.
- Lugares con autoría: los que agrega un admin o vienen en el seed quedan "Verificados por Rutea"; los que agrega cualquier otra cuenta quedan marcados con su nombre, como aporte de la comunidad.
- **Cómo llegar** calcula la ruta dentro de la app (sin salir a Google/Apple Maps); el enlace a Maps queda como opción secundaria.
- **Admin** (`/admin`): métricas, revocar/reactivar keys, cambiar su cuota, deshabilitar usuarios, alta y baja de lugares.
- Al primer arranque se cargan ~42 lugares de ejemplo en México (categorías pensadas para no saturar el mapa: naturaleza, comida, cultura, carretera… los de transporte/otros quedan ocultos por defecto en los filtros, pero siguen ahí).

Uso de la API: ver `/docs` en la app. Cabecera: `X-API-Key: tr_…` (o `Authorization: Bearer tr_…`).

## Configuración

Todo vive en `.env` (lo crea `setup.sh`): puerto, credenciales, `ANON_REQUESTS_PER_MINUTE`, `API_KEY_DAILY_LIMIT`, `API_KEY_MAX_PER_USER`. Tras editarlo, vuelve a ejecutar `./setup.sh`.

La contraseña del admin se sincroniza desde `.env` en cada arranque del backend.

El ruteo por carretera usa por defecto el servidor demo público de OSRM (gratis, sin key, sin SLA). Para apuntar a un OSRM propio, agrega `OSRM_URL=...` al `environment` del servicio `backend` en `docker-compose.yml` (ver `pois.routing.*` en `backend/src/main/resources/application.properties`).

## Importar lugares reales de OpenStreetMap

El seed de ~42 lugares es solo demo (escrito a mano). Para llenar la base con datos reales
de México (gasolineras, restaurantes, museos, hoteles, miradores, templos…) hay un
importador que descarga de [Overpass API](https://overpass-api.de) (datos de OpenStreetMap,
licencia [ODbL](https://www.openstreetmap.org/copyright)) y genera un `seed/pois.json` nuevo:

```bash
python3 tools/import_osm_pois.py
```

Solo necesita Python 3 (sin instalar nada) e internet. Con México completo puede tardar
entre 30 y 90+ minutos — Overpass es un servicio público compartido, el script le da ~2s
de cortesía entre consulta y consulta para no saturarlo. Se puede interrumpir con Ctrl+C:
guarda avance parcial en el archivo después de cada estado, así que no se pierde todo.

Para probar rápido con un alcance chico antes de lanzar el país completo:

```bash
python3 tools/import_osm_pois.py --states MX-VER,MX-PUE --tags museum,fuel,restaurant
```

Cuando termine, revisa cuántos lugares bajó y vuelve a levantar todo desde cero para que
se siembren (esto borra los datos actuales de la base, incluidas cuentas y API keys creadas
en local):

```bash
python3 -c "import json;print(len(json.load(open('backend/src/main/resources/seed/pois.json'))))"
docker compose down -v
./setup.sh
```

El sembrado corre en segundo plano al arrancar el backend (no bloquea el arranque ni el
healthcheck, aunque sean cientos de miles de filas) — revisa el progreso con:

```bash
docker compose logs -f backend | grep -i sembrando
```

Los tipos de OSM que importa (y los que deja fuera a propósito para no saturar el mapa,
como `parking`, `atm`, `bank`, gimnasios o escuelas) están en `TAG_MAP` dentro del script;
agregar una categoría nueva es agregar una línea ahí.

## Desarrollo local

```bash
cd backend && DB_URL=jdbc:postgresql://localhost:5432/pois DB_USER=pois DB_PASS=… JWT_SECRET=$(openssl rand -hex 32) mvn spring-boot:run
cd frontend && npm install && npm run dev   # proxy /api -> localhost:8082
```
