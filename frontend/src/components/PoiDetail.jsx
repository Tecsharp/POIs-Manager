import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { X, MapPin, Phone, Globe, Clock, CreditCard, Star, Navigation, ThumbsUp, ThumbsDown, BadgeCheck, UserRound, ExternalLink } from 'lucide-react'
import { api, ApiError } from '../lib/api'
import { useAuth } from '../lib/auth'
import { typeLabel } from '../lib/poiTypes'
import IconTile from './IconTile'

const DAYS = { monday: 'lunes', tuesday: 'martes', wednesday: 'miércoles', thursday: 'jueves', friday: 'viernes', saturday: 'sábado', sunday: 'domingo' }

export default function PoiDetail({ id, onClose, onDirections }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [poi, setPoi] = useState(null)
  const [error, setError] = useState('')
  const [votes, setVotes] = useState(null)
  const [voting, setVoting] = useState(false)
  const [directions, setDirections] = useState(null)
  const [directionsError, setDirectionsError] = useState('')
  const [findingRoute, setFindingRoute] = useState(false)

  useEffect(() => {
    setPoi(null)
    setError('')
    setVotes(null)
    setDirections(null)
    setDirectionsError('')
    onDirections?.(null)
    api(`/pois/${id}`, { auth: !!user }).then((r) => {
      const p = r.data.object
      setPoi(p)
      setVotes({ upvotes: p.upvotes ?? 0, downvotes: p.downvotes ?? 0, myVote: p.my_vote ?? null })
    }).catch((e) => setError(e.message))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  const castVote = async (value) => {
    if (!user) return navigate('/login', { state: { from: window.location.pathname } })
    setVoting(true)
    try {
      const remove = votes?.myVote === value
      const summary = remove
        ? await api(`/pois/${id}/vote`, { method: 'DELETE' })
        : await api(`/pois/${id}/vote`, { method: 'POST', body: { value } })
      setVotes({ upvotes: summary.upvotes, downvotes: summary.downvotes, myVote: summary.my_vote })
    } catch (e) {
      setError(e.message)
    } finally {
      setVoting(false)
    }
  }

  const findRoute = () => {
    if (!navigator.geolocation) return setDirectionsError('Tu navegador no permite geolocalización.')
    setFindingRoute(true)
    setDirectionsError('')
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const origin = { lat: pos.coords.latitude, lng: pos.coords.longitude }
        try {
          const qs = new URLSearchParams({ from_lat: origin.lat, from_lng: origin.lng, to_lat: poi.lat, to_lng: poi.lng })
          const result = await api(`/pois/directions?${qs}`, { auth: !!user })
          const withOrigin = { ...result, origin }
          setDirections(withOrigin)
          onDirections?.(withOrigin)
        } catch (e) {
          setDirectionsError(e instanceof ApiError ? e.message : 'No se pudo calcular la ruta.')
        } finally {
          setFindingRoute(false)
        }
      },
      () => {
        setDirectionsError('No pudimos obtener tu ubicación. Revisa los permisos del navegador.')
        setFindingRoute(false)
      },
    )
  }

  return (
    <section className="card p-4" aria-live="polite">
      <div className="flex items-start gap-3">
        {poi && <IconTile type={poi.type} size={44} />}
        <div className="min-w-0 flex-1">
          {poi ? (
            <>
              <h3 className="text-[20px] font-semibold leading-tight">{poi.name}</h3>
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
          <p className="mt-2 flex items-center gap-1.5 text-[12px] text-label-2">
            {poi.verified ? (
              <><BadgeCheck className="size-3.5 text-accent" aria-hidden />Verificado por Rutea</>
            ) : poi.created_by ? (
              <><UserRound className="size-3.5" aria-hidden />Agregado por {poi.created_by}</>
            ) : (
              <><UserRound className="size-3.5" aria-hidden />Aportado por la comunidad</>
            )}
          </p>

          {poi.description && <p className="mt-3 text-[15px] leading-relaxed">{poi.description}</p>}

          <div className="mt-3 flex items-center gap-2">
            <VoteButton icon={ThumbsUp} active={votes?.myVote === 1} count={votes?.upvotes ?? 0} label="Útil" disabled={voting} onClick={() => castVote(1)} />
            <VoteButton icon={ThumbsDown} active={votes?.myVote === -1} count={votes?.downvotes ?? 0} label="No útil" disabled={voting} onClick={() => castVote(-1)} />
          </div>

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

          {directions ? (
            <div className="mt-4 rounded-xl bg-fill/50 p-3 text-[14px]">
              <p className="font-semibold">
                {directions.distance_km} km{directions.duration_min != null && ` · ${Math.round(directions.duration_min)} min`}
              </p>
              <p className="text-[12px] text-label-2">
                {directions.routed ? 'Ruta por carretera en el mapa' : 'No se pudo calcular la carretera; se muestra línea recta'}
              </p>
            </div>
          ) : (
            <button onClick={findRoute} disabled={findingRoute} className="btn-primary mt-4 w-full">
              <Navigation className="size-4" aria-hidden /> {findingRoute ? 'Calculando ruta…' : 'Cómo llegar'}
            </button>
          )}
          {directionsError && <p role="alert" className="mt-2 text-[12px] text-danger">{directionsError}</p>}

          <a className="mt-2 flex items-center justify-center gap-1.5 py-2 text-[12px] text-label-2 hover:text-accent" target="_blank" rel="noreferrer"
            href={`https://www.google.com/maps/dir/?api=1&destination=${poi.lat},${poi.lng}`}>
            <ExternalLink className="size-3.5" aria-hidden /> Abrir en Google Maps
          </a>
        </>
      )}
    </section>
  )
}

function VoteButton({ icon: Icon, active, count, label, disabled, onClick }) {
  return (
    <button onClick={onClick} disabled={disabled} aria-pressed={active} aria-label={label}
      className={`flex cursor-pointer items-center gap-1.5 rounded-lg px-3 py-1.5 text-[13px] font-semibold transition-colors duration-150 disabled:cursor-wait ${active ? 'bg-accent/15 text-accent' : 'bg-fill text-label-2 hover:text-label'}`}>
      <Icon className="size-3.5" aria-hidden />
      {count}
    </button>
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
