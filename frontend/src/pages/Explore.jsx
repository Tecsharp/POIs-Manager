import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { LocateFixed, Search, MousePointerClick } from 'lucide-react'
import PoiMap from '../components/PoiMap'
import CategoryFilter from '../components/CategoryFilter'
import PoiListItem from '../components/PoiListItem'
import PoiDetail from '../components/PoiDetail'
import { api } from '../lib/api'
import { DESTINATIONS, typesFor } from '../lib/poiTypes'

export default function Explore() {
  const [params] = useSearchParams()
  const initial = params.get('lat')
    ? { lat: +params.get('lat'), lng: +params.get('lng'), name: params.get('name') || 'Punto elegido' }
    : DESTINATIONS[0]
  const [center, setCenter] = useState(initial)
  const [radius, setRadius] = useState(25)
  const [cats, setCats] = useState([])
  const [text, setText] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(null)

  const search = useCallback(async () => {
    setLoading(true)
    setError('')
    const qs = new URLSearchParams({ lat: center.lat, lng: center.lng, radius_km: radius, limit: 200 })
    typesFor(cats).forEach((t) => qs.append('type', t))
    try {
      setResult(await api(`/pois/nearby?${qs}`, { auth: false }))
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [center, radius, cats])

  useEffect(() => {
    const t = setTimeout(search, 250)
    return () => clearTimeout(t)
  }, [search])

  const pois = useMemo(() => {
    const q = text.trim().toLowerCase()
    const list = result?.pois ?? []
    return q ? list.filter((p) => `${p.name} ${p.address ?? ''}`.toLowerCase().includes(q)) : list
  }, [result, text])

  const locate = () => {
    if (!navigator.geolocation) return setError('Tu navegador no permite geolocalización')
    navigator.geolocation.getCurrentPosition(
      (p) => setCenter({ lat: p.coords.latitude, lng: p.coords.longitude, name: 'Mi ubicación' }),
      () => setError('No pudimos obtener tu ubicación. Revisa los permisos del navegador.'),
    )
  }

  return (
    <div className="grid h-[calc(100dvh-3rem)] lg:grid-cols-[400px_1fr]">
      <aside className="order-2 flex min-h-0 flex-col border-r border-sep bg-surface-2 lg:order-1">
        <div className="space-y-4 border-b border-sep p-5">
          <div>
            <p className="eyebrow">Cerca de</p>
            <h1 className="text-[28px] leading-tight">{center.name}</h1>
          </div>
          <div className="no-scrollbar -mx-5 flex gap-2 overflow-x-auto px-5">
            <button onClick={locate} className="capsule text-accent"><LocateFixed className="size-3.5" aria-hidden />Mi ubicación</button>
            {DESTINATIONS.map((d) => (
              <button key={d.name} onClick={() => setCenter(d)} aria-pressed={center.name === d.name} className="capsule">{d.name}</button>
            ))}
          </div>
          <div>
            <div className="flex justify-between text-[13px]">
              <label htmlFor="radius" className="text-label-2">Radio de búsqueda</label><span className="tabular font-medium">{radius} km</span>
            </div>
            <input id="radius" type="range" min="1" max="100" value={radius} onChange={(e) => setRadius(+e.target.value)} className="mt-2 w-full cursor-pointer accent-[var(--accent)]" />
          </div>
          <CategoryFilter selected={cats} onChange={setCats} />
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-label-3" aria-hidden />
            <input className="input !h-9 !pl-9" aria-label="Filtrar por nombre o dirección" placeholder="Buscar en resultados" value={text} onChange={(e) => setText(e.target.value)} />
          </div>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto p-2">
          {selected && <div className="mb-2"><PoiDetail id={selected} onClose={() => setSelected(null)} /></div>}
          <p className="px-3 pb-1 pt-2 text-[13px] text-label-2" role="status">
            {loading ? 'Buscando…' : error ? '' : `${pois.length} ${pois.length === 1 ? 'lugar' : 'lugares'} en ${radius} km`}
          </p>
          {error && <p role="alert" className="mx-2 rounded-xl bg-danger/10 p-3 text-[15px] text-danger">{error}</p>}
          {!loading && !error && pois.length === 0 && (
            <div className="px-6 py-12 text-center">
              <MousePointerClick className="mx-auto size-7 text-label-3" aria-hidden />
              <p className="mt-3 text-[15px] font-medium">Sin resultados aquí</p>
              <p className="mt-1 text-[13px] text-label-2">Amplía el radio, quita filtros o toca otro punto del mapa.</p>
            </div>
          )}
          <div>
            {pois.map((p) => <PoiListItem key={p.id} poi={p} active={p.id === selected} onClick={() => setSelected(p.id)} />)}
          </div>
        </div>
      </aside>

      <div className="relative order-1 h-[45dvh] lg:order-2 lg:h-auto">
        <PoiMap
          pois={pois}
          selectedId={selected}
          onSelect={(p) => setSelected(p.id)}
          onMapClick={({ lat, lng }) => setCenter({ lat, lng, name: `${lat.toFixed(3)}, ${lng.toFixed(3)}` })}
          center={[center.lat, center.lng]}
          zoom={radius > 40 ? 8 : radius > 15 ? 10 : 12}
          radiusKm={radius}
        />
        <p className="pointer-events-none absolute left-1/2 top-3 z-[500] -translate-x-1/2 whitespace-nowrap rounded-full bg-surface/90 px-3.5 py-1.5 text-[12px] text-label-2 shadow-sm ring-1 ring-sep backdrop-blur">
          Toca el mapa para buscar en otro punto
        </p>
      </div>
    </div>
  )
}
