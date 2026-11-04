import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Undo2, Trash2, Route as RouteIcon, Code2 } from 'lucide-react'
import PoiMap from '../components/PoiMap'
import CategoryFilter from '../components/CategoryFilter'
import PoiListItem from '../components/PoiListItem'
import PoiDetail from '../components/PoiDetail'
import CodeBlock from '../components/CodeBlock'
import { api, apiBaseUrl } from '../lib/api'
import { categoryOf, typesFor, DEFAULT_CATEGORIES } from '../lib/poiTypes'

const PRESETS = {
  'cdmx-veracruz': { name: 'CDMX → Veracruz', points: [[19.4326, -99.1332], [19.0414, -98.2063], [18.85, -97.1], [19.5438, -96.9102], [19.1903, -96.1533]] },
  'cdmx-oaxaca': { name: 'CDMX → Oaxaca', points: [[19.4326, -99.1332], [19.0414, -98.2063], [18.4627, -97.3928], [17.0732, -96.7266]] },
  'bajio': { name: 'Querétaro → Guanajuato', points: [[20.5888, -100.3899], [20.7436, -99.9419], [20.9144, -100.7452], [21.019, -101.2574]] },
  'sur': { name: 'CDMX → Taxco', points: [[19.4326, -99.1332], [18.9867, -99.0997], [18.6686, -99.5097], [18.5564, -99.6051]] },
}

export default function RoutePlanner() {
  const [params] = useSearchParams()
  const [points, setPoints] = useState(PRESETS[params.get('preset')]?.points ?? [])
  const [buffer, setBuffer] = useState(10)
  const [cats, setCats] = useState(DEFAULT_CATEGORIES)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(null)
  const [showCode, setShowCode] = useState(false)
  const [directions, setDirections] = useState(null)

  const body = {
    waypoints: points.map(([lat, lng]) => ({ lat: +lat.toFixed(5), lng: +lng.toFixed(5) })),
    buffer_km: buffer,
    max_results: 200,
    ...(cats.length ? { types: typesFor(cats) } : {}),
  }

  const run = async () => {
    if (points.length < 2) return
    setLoading(true)
    setError('')
    try {
      setResult(await api('/pois/route', { method: 'POST', body, auth: false }))
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  // Re-busca al cambiar ruta, ancho o filtros (con pausa para no saturar mientras se arrastra el slider).
  useEffect(() => {
    if (points.length < 2) { setResult(null); return }
    const t = setTimeout(run, 350)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [JSON.stringify(points), buffer, JSON.stringify(cats)])

  const loadPreset = (key) => { setSelected(null); setDirections(null); setPoints(PRESETS[key].points) }

  const routeLine = result?.routed && result?.route_geometry?.length > 1
    ? result.route_geometry.map((p) => [p.lat, p.lng])
    : points

  const summary = Object.values(Object.entries(result?.by_type ?? {}).reduce((acc, [t, n]) => {
    const c = categoryOf(t)
    acc[c.id] = { cat: c, n: (acc[c.id]?.n ?? 0) + n }
    return acc
  }, {})).sort((x, y) => y.n - x.n)

  return (
    <div className="grid h-[calc(100dvh-3rem)] lg:grid-cols-[420px_1fr]">
      <aside className="order-2 flex min-h-0 flex-col border-r border-sep bg-surface-2 lg:order-1">
        <div className="space-y-4 border-b border-sep p-5">
          <div>
            <p className="eyebrow">Planear ruta</p>
            <h1 className="text-[28px] leading-tight">¿Qué hay en mi camino?</h1>
            <p className="mt-1 text-[13px] text-label-2">Toca el mapa para agregar paradas en orden.</p>
          </div>
          <div className="no-scrollbar -mx-5 flex gap-2 overflow-x-auto px-5">
            {Object.entries(PRESETS).map(([k, p]) => (
              <button key={k} onClick={() => loadPreset(k)} className="capsule">{p.name}</button>
            ))}
          </div>
          <div className="flex items-center justify-between">
            <span className="tabular text-[13px] text-label-2">{points.length} {points.length === 1 ? 'parada' : 'paradas'}</span>
            <div className="flex gap-2">
              <button className="btn-secondary btn-sm" disabled={!points.length} onClick={() => setPoints(points.slice(0, -1))}><Undo2 className="size-3.5" aria-hidden />Deshacer</button>
              <button className="btn-secondary btn-sm" disabled={!points.length} onClick={() => { setPoints([]); setSelected(null) }}><Trash2 className="size-3.5" aria-hidden />Limpiar</button>
            </div>
          </div>
          <div>
            <div className="flex justify-between text-[13px]">
              <label htmlFor="buffer" className="text-label-2">Desvío máximo</label><span className="tabular font-medium">{buffer} km</span>
            </div>
            <input id="buffer" type="range" min="1" max="50" value={buffer} onChange={(e) => setBuffer(+e.target.value)} className="mt-2 w-full cursor-pointer accent-[var(--accent)]" />
          </div>
          <CategoryFilter selected={cats} onChange={setCats} />
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto p-2">
          {points.length < 2 && (
            <div className="px-6 py-14 text-center">
              <RouteIcon className="mx-auto size-8 text-accent" strokeWidth={1.8} aria-hidden />
              <p className="mt-3 text-[17px] font-semibold">Traza tu camino</p>
              <p className="mt-1 text-[13px] text-label-2">Marca al menos dos puntos en el mapa o elige una ruta de ejemplo.</p>
            </div>
          )}
          {error && <p role="alert" className="m-2 rounded-xl bg-danger/10 p-3 text-[15px] text-danger">{error}</p>}
          {result && points.length >= 2 && (
            <>
              <dl className="mx-2 mt-1 grid grid-cols-3 divide-x divide-sep rounded-xl bg-surface py-3 text-center ring-1 ring-sep">
                <Stat label="Distancia" value={`${result.route_length_km} km`} />
                <Stat label="Desvío" value={`${result.buffer_km} km`} />
                <Stat label="Lugares" value={loading ? '…' : result.total} />
              </dl>
              <p className="flex items-center gap-1.5 px-3 pb-1 pt-2 text-[12px] text-label-2">
                <RouteIcon className="size-3" aria-hidden />
                {result.routed ? 'Ruta real por carretera' : 'Línea recta — no se encontró ruta por carretera'}
              </p>
              {summary.length > 0 && (
                <p className="px-3 pb-1 pt-1 text-[13px] text-label-2">
                  {summary.map(({ cat, n }) => `${n} ${cat.label.toLowerCase()}`).join(' · ')}
                </p>
              )}
              {selected && <div className="my-2"><PoiDetail id={selected} onClose={() => { setSelected(null); setDirections(null) }} onDirections={setDirections} /></div>}
              <div className="mt-1">
                {result.pois.map((p) => <PoiListItem key={p.id} poi={p} active={p.id === selected} onClick={() => setSelected(p.id)} />)}
              </div>
              {result.total === 0 && <p className="p-6 text-center text-[13px] text-label-2">Nada en este corredor. Prueba con un desvío mayor o quita filtros.</p>}
              <button onClick={() => setShowCode(!showCode)} className="mx-3 mt-4 flex cursor-pointer items-center gap-1.5 text-[13px] text-accent hover:underline">
                <Code2 className="size-4" aria-hidden />{showCode ? 'Ocultar' : 'Ver'} esta consulta como petición a la API
              </button>
              {showCode && (
                <div className="p-2">
                  <CodeBlock label="Terminal" code={`curl -X POST ${apiBaseUrl()}/pois/route \\\n  -H "X-API-Key: tr_tu_api_key" \\\n  -H "Content-Type: application/json" \\\n  -d '${JSON.stringify(body)}'`} />
                </div>
              )}
            </>
          )}
        </div>
      </aside>

      <div className="relative order-1 h-[45dvh] lg:order-2 lg:h-auto">
        <PoiMap
          pois={result && points.length >= 2 ? result.pois : []}
          selectedId={selected}
          onSelect={(p) => setSelected(p.id)}
          onMapClick={({ lat, lng }) => points.length < 50 && setPoints([...points, [lat, lng]])}
          route={points.length > 1 ? routeLine : []}
          waypoints={points}
          fitPoints={points.length > 1 ? routeLine : null}
          center={points[0] ?? [19.4326, -99.1332]}
          zoom={points.length ? 9 : 7}
          directions={directions}
        />
      </div>
    </div>
  )
}

function Stat({ label, value }) {
  return (
    <div>
      <dt className="text-[12px] text-label-2">{label}</dt>
      <dd className="tabular mt-0.5 text-[17px] font-semibold">{value}</dd>
    </div>
  )
}
