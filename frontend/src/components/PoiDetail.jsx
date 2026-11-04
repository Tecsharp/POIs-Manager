import { useEffect, useState } from 'react'
import { X, MapPin, Phone, Globe, Clock, CreditCard, Star, Navigation } from 'lucide-react'
import { api } from '../lib/api'
import { typeLabel } from '../lib/poiTypes'
import IconTile from './IconTile'

const DAYS = { monday: 'lunes', tuesday: 'martes', wednesday: 'miércoles', thursday: 'jueves', friday: 'viernes', saturday: 'sábado', sunday: 'domingo' }

export default function PoiDetail({ id, onClose }) {
  const [poi, setPoi] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    setPoi(null)
    setError('')
    api(`/pois/${id}`, { auth: false }).then((r) => setPoi(r.data.object)).catch((e) => setError(e.message))
  }, [id])

  return (
    <section className="card p-4" aria-live="polite">
      <div className="flex items-start gap-3">
        {poi && <IconTile type={poi.type} size={44} />}
        <div className="min-w-0 flex-1">
          {poi ? (
            <>
              <h3 className="text-[20px] leading-tight">{poi.name}</h3>
              <p className="mt-0.5 flex items-center gap-1.5 text-[13px] text-label-2">
                {typeLabel(poi.type)}
                {poi.rating != null && <><span aria-hidden>·</span><Star className="size-3 fill-current text-warning" aria-hidden />{poi.rating.toFixed(1)}</>}
              </p>
            </>
          ) : <p className="py-2 text-[15px] text-label-2">{error || 'Cargando…'}</p>}
        </div>
        <button onClick={onClose} className="grid size-8 shrink-0 cursor-pointer place-items-center rounded-full bg-fill text-label-2 hover:bg-fill-strong" aria-label="Cerrar detalle">
          <X className="size-4" />
        </button>
      </div>
      {poi && (
        <>
          {poi.description && <p className="mt-3 text-[15px] leading-relaxed">{poi.description}</p>}
          <dl className="mt-3 divide-y divide-sep rounded-xl bg-fill/50 text-[15px]">
            {poi.address && <Row icon={MapPin} label="Dirección">{poi.address}</Row>}
            {poi.phone && <Row icon={Phone} label="Teléfono"><a href={`tel:${poi.phone}`} className="text-accent">{poi.phone}</a></Row>}
            {poi.website && <Row icon={Globe} label="Sitio web"><a href={poi.website} target="_blank" rel="noreferrer" className="break-all text-accent">{poi.website}</a></Row>}
            {poi.hours && (
              <Row icon={Clock} label="Horario">
                {poi.hours.open_time && `${poi.hours.open_time} – ${poi.hours.close_time}`}
                {poi.hours.closed_days?.length > 0 && <span className="text-label-2"> · cierra {poi.hours.closed_days.map((d) => DAYS[d] ?? d).join(', ')}</span>}
                {poi.hours.comments && <span className="block text-label-2">{poi.hours.comments}</span>}
              </Row>
            )}
            {poi.accepts_cards != null && <Row icon={CreditCard} label="Pago">{poi.accepts_cards ? 'Acepta tarjeta' : 'Solo efectivo'}</Row>}
          </dl>
          {poi.specialties?.length > 0 && (
            <p className="mt-3 text-[13px] text-label-2">{poi.specialties.join(' · ')}</p>
          )}
          <a className="btn-primary mt-4 w-full" target="_blank" rel="noreferrer"
            href={`https://maps.apple.com/?daddr=${poi.lat},${poi.lng}`}>
            <Navigation className="size-4" aria-hidden /> Cómo llegar
          </a>
        </>
      )}
    </section>
  )
}

function Row({ icon: Icon, label, children }) {
  return (
    <div className="flex gap-3 px-3 py-2.5">
      <Icon className="mt-1 size-4 shrink-0 text-label-2" aria-hidden />
      <dt className="sr-only">{label}</dt>
      <dd className="min-w-0">{children}</dd>
    </div>
  )
}
