import { MapContainer, TileLayer, Marker, Popup, Polyline, Circle, useMap, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import { useEffect, useMemo } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { categoryOf, typeLabel } from '../lib/poiTypes'

const iconCache = new Map()
// Marcador de lugar: círculo del color de la categoría con borde blanco.
function poiIcon(type, active) {
  const key = `${type}-${active}`
  if (!iconCache.has(key)) {
    const cat = categoryOf(type)
    const Icon = cat.icon
    const size = active ? 40 : 28
    const html = renderToStaticMarkup(
      <div style={{
        width: size, height: size, background: cat.color, borderRadius: '50%', display: 'grid', placeItems: 'center',
        border: '2px solid #fff', color: '#fff', boxShadow: '0 2px 6px rgba(0,0,0,.3)', transition: 'all .2s',
      }}>
        <Icon size={active ? 20 : 14} strokeWidth={2.4} />
      </div>,
    )
    iconCache.set(key, L.divIcon({ html, className: 'poi-pin', iconSize: [size, size], iconAnchor: [size / 2, size / 2], popupAnchor: [0, -size / 2] }))
  }
  return iconCache.get(key)
}

const ROUTE_COLOR = '#1a8a73' // verde-azulado de marca: distingue la ruta del naranja de la interfaz
const DIRECTIONS_COLOR = '#b84a1f'

const waypointIcon = (n) => L.divIcon({
  className: 'poi-pin',
  iconSize: [24, 24],
  iconAnchor: [12, 12],
  html: `<div style="width:24px;height:24px;border-radius:999px;background:var(--surface);color:var(--label);border:2.5px solid ${ROUTE_COLOR};display:grid;place-items:center;font:600 11px/1 var(--font-sans);box-shadow:0 2px 6px rgba(0,0,0,.25)">${n}</div>`,
})

const meIcon = L.divIcon({
  className: 'poi-pin',
  iconSize: [20, 20],
  iconAnchor: [10, 10],
  html: `<div style="width:20px;height:20px;border-radius:999px;background:${DIRECTIONS_COLOR};border:3px solid var(--surface);box-shadow:0 2px 8px rgba(0,0,0,.35)"></div>`,
})

function ClickHandler({ onClick }) {
  useMapEvents({ click: (e) => onClick?.({ lat: e.latlng.lat, lng: e.latlng.lng }) })
  return null
}

function FitTo({ points, center, zoom }) {
  const map = useMap()
  const key = JSON.stringify(points ?? center)
  useEffect(() => {
    map.invalidateSize() // el contenedor puede cambiar de tamaño tras el primer render del grid
    if (points && points.length > 1) map.fitBounds(L.latLngBounds(points), { padding: [48, 48], maxZoom: 13 })
    else if (center) map.setView(center, zoom ?? map.getZoom(), { animate: true })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key])
  return null
}

/** Encuadra la ruta de "cómo llegar" apenas llega, sin pelear con el FitTo principal. */
function FitToDirections({ geometry }) {
  const map = useMap()
  const key = JSON.stringify(geometry)
  useEffect(() => {
    if (geometry && geometry.length > 1) map.fitBounds(L.latLngBounds(geometry), { padding: [64, 64], maxZoom: 15 })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key])
  return null
}

export default function PoiMap({
  pois = [], selectedId, onSelect, onMapClick, center = [19.4326, -99.1332], zoom = 11,
  route, waypoints, radiusKm, fitPoints, directions, className = '',
}) {
  const markers = useMemo(() => pois.map((p) => (
    <Marker key={p.id} position={[p.lat, p.lng]} icon={poiIcon(p.type, p.id === selectedId)}
      zIndexOffset={p.id === selectedId ? 1000 : 0}
      eventHandlers={{ click: () => onSelect?.(p) }}>
      <Popup>
        <div className="text-[13px] font-semibold">{p.name}</div>
        <div className="text-[12px] text-label-2">{typeLabel(p.type)}{p.distance_km != null && ` · ${p.distance_km} km`}</div>
      </Popup>
    </Marker>
  )), [pois, selectedId, onSelect])

  return (
    <MapContainer center={center} zoom={zoom} className={`h-full w-full ${className}`} scrollWheelZoom>
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        maxZoom={19}
      />
      <ClickHandler onClick={onMapClick} />
      <FitTo points={fitPoints} center={fitPoints ? null : center} zoom={zoom} />
      {radiusKm && center && (
        <Circle center={center} radius={radiusKm * 1000} pathOptions={{ color: ROUTE_COLOR, weight: 1.5, fillOpacity: 0.06 }} />
      )}
      {route && route.length > 0 && (
        <>
          <Polyline positions={route} pathOptions={{ color: ROUTE_COLOR, weight: 16, opacity: 0.12, lineCap: 'round', lineJoin: 'round' }} />
          <Polyline positions={route} pathOptions={{ color: ROUTE_COLOR, weight: 5, lineCap: 'round', lineJoin: 'round' }} />
        </>
      )}
      {waypoints?.map((p, i) => <Marker key={`wp-${i}`} position={p} icon={waypointIcon(i + 1)} />)}
      {directions?.geometry?.length > 1 && (
        <>
          <FitToDirections geometry={directions.geometry} />
          <Polyline positions={directions.geometry} pathOptions={{ color: DIRECTIONS_COLOR, weight: 5, opacity: 0.9, dashArray: directions.routed ? undefined : '2 10', lineCap: 'round' }} />
          {directions.origin && <Marker position={[directions.origin.lat, directions.origin.lng]} icon={meIcon} />}
        </>
      )}
      {markers}
    </MapContainer>
  )
}
