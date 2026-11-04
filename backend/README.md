# 🏍️ Triburide POI API

API pública geoespacial para consultar **Puntos de Interés** (talleres, gasolineras, spots fotográficos, hoteles, comida) a lo largo de rutas de motociclismo.

Los POIs no están casados con rutas específicas — se consultan dinámicamente según el **corredor geográfico** de cualquier ruta que se envíe.

---

## ¿Cómo funciona?

```
Tu ruta (polyline) → Buffer zone (corredor) → POIs dentro del corredor → Respuesta ordenada
```

Si vas de **Tizayuca a Veracruz** o de **San Luis Potosí a Veracruz**, y tus rutas comparten 200 km en la carretera 140, los POIs en esa zona aparecen en **ambas consultas** sin que nadie haya registrado esas rutas específicas.

---

## Base URL

```
https://api.triburide.com/pois
```

---

## Autenticación

Todas las peticiones requieren un API key en el header:

```http
Authorization: Bearer TR_API_KEY_AQUI
```

Obtén tu API key en [developers.triburide.com](https://developers.triburide.com).

---

## Inicio rápido

### 1. Buscar POIs a lo largo de una ruta

```bash
curl -X POST https://api.triburide.com/v1/route/pois \
  -H "Authorization: Bearer TU_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "waypoints": [
      { "lat": 20.0567, "lng": -98.7896 },
      { "lat": 19.9167, "lng": -97.9833 },
      { "lat": 19.4326, "lng": -96.9500 }
    ],
    "buffer_km": 8,
    "types": ["taller", "gasolinera", "spot"]
  }'
```

### 2. Buscar POIs cerca de un punto

```bash
curl "https://api.triburide.com/v1/pois/nearby?lat=19.4326&lng=-96.9500&radius_km=15&type=taller" \
  -H "Authorization: Bearer TU_API_KEY"
```

### 3. Registrar un nuevo POI

```bash
curl -X POST https://api.triburide.com/v1/pois \
  -H "Authorization: Bearer TU_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Taller Moto Express",
    "type": "taller",
    "lat": 19.4350,
    "lng": -96.9480,
    "address": "Av. Principal 123, Veracruz",
    "phone": "+52 229 123 4567",
    "hours": {
      "lunes": "08:00-18:00",
      "martes": "08:00-18:00",
      "sabado": "09:00-14:00",
      "domingo": "cerrado"
    },
    "specialties": ["frenos", "suspension", "llantas"]
  }'
```

---

## Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/route/pois` | POIs en corredor de ruta ⭐ |
| `GET` | `/pois/nearby` | POIs cerca de un punto |
| `GET` | `/pois` | Listar y filtrar POIs |
| `GET` | `/pois/{id}` | Detalle de un POI |
| `POST` | `/pois` | Registrar nuevo POI |
| `PUT` | `/pois/{id}` | Actualizar POI |
| `DELETE` | `/pois/{id}` | Eliminar POI |
| `POST` | `/pois/{id}/reviews` | Agregar reseña |
| `GET` | `/pois/{id}/reviews` | Ver reseñas de un POI |
| `POST` | `/pois/{id}/report` | Reportar POI inactivo/incorrecto |
| `GET` | `/types` | Tipos de POI disponibles |

Documentación completa: [docs.triburide.com/poi-api](https://docs.triburide.com/poi-api)

---

## Tipos de POI soportados

| Tipo | Descripción |
|------|-------------|
| `taller` | Talleres mecánicos para motos |
| `gasolinera` | Gasolineras (incluye si tienen servicio para motos) |
| `spot` | Puntos fotográficos o miradores |
| `hotel` | Hoteles con estacionamiento para motos |
| `comida` | Restaurantes, taquerías, fondas |
| `camping` | Zonas de campamento |
| `hospital` | Clínicas y hospitales de emergencia |
| `concesionario` | Distribuidores de motos |
| `otro` | Otros puntos de interés |

---

## Rate Limits

| Plan | Límite |
|------|--------|
| Free | 100 req/día |
| Developer | 5,000 req/día |
| Business | 50,000 req/día |
| Enterprise | Ilimitado (contactar) |

Headers de respuesta incluyen:
```
X-RateLimit-Limit: 5000
X-RateLimit-Remaining: 4873
X-RateLimit-Reset: 1735689600
```

---

## SDKs

- **JavaScript/TypeScript**: `npm install @triburide/poi-sdk`
- **Python**: `pip install triburide-poi`
- **Swift (iOS)**: Swift Package Manager
- **Kotlin (Android)**: Maven Central

---

## Soporte

- 📧 api@triburide.com
- 💬 Discord: [discord.gg/triburide](https://discord.gg/triburide)
- 🐛 Issues: [github.com/triburide/poi-api](https://github.com/triburide/poi-api)

---

## Licencia

API pública bajo [Términos de Uso de Triburide](https://triburide.com/terms).  
Los datos de POIs son contribución de la comunidad bajo licencia [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).

