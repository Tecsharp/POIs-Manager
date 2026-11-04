import { useState } from 'react'
import { api } from '../lib/api'
import { ALL_TYPES, typeLabel } from '../lib/poiTypes'
import PoiMap from './PoiMap'

const EMPTY = { name: '', type: 'SPOT', lat: '', lng: '', address: '', description: '', rating: '', phone: '', website: '' }

/** Formulario para dar de alta un lugar. Lo usan tanto /agregar (cualquier cuenta) como el panel de admin. */
export default function PoiForm({ onCreated, initialCenter = [23.6, -102.5], initialZoom = 5 }) {
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setMsg('')
    setBusy(true)
    const body = Object.fromEntries(Object.entries(form).filter(([, v]) => v !== ''))
    body.lat = +form.lat
    body.lng = +form.lng
    if (form.rating) body.rating = +form.rating
    try {
      const created = await api('/pois', { method: 'POST', body })
      setForm(EMPTY)
      setMsg('¡Lugar agregado! Gracias por contribuir.')
      onCreated?.(created)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const point = form.lat && form.lng ? [{ id: 'new', name: form.name || 'Nuevo lugar', type: form.type, lat: +form.lat, lng: +form.lng }] : []

  return (
    <form onSubmit={submit} className="card space-y-4 p-6">
      <div className="h-56 overflow-hidden rounded-2xl">
        <PoiMap pois={point} zoom={initialZoom} center={initialCenter}
          onMapClick={({ lat, lng }) => setForm((f) => ({ ...f, lat: lat.toFixed(6), lng: lng.toFixed(6) }))} />
      </div>
      <p className="text-[12px] text-label-2">Toca el mapa para fijar las coordenadas.</p>
      <div className="grid grid-cols-2 gap-3">
        <Field label="Nombre" className="col-span-2"><input className="input" required maxLength={200} value={form.name} onChange={set('name')} /></Field>
        <Field label="Tipo">
          <select className="input" value={form.type} onChange={set('type')}>
            {ALL_TYPES.map((t) => <option key={t} value={t}>{typeLabel(t)}</option>)}
          </select>
        </Field>
        <Field label="Calificación (1–5)"><input className="input" type="number" min="1" max="5" step="0.1" value={form.rating} onChange={set('rating')} /></Field>
        <Field label="Latitud"><input className="input" required type="number" step="any" min="-90" max="90" value={form.lat} onChange={set('lat')} /></Field>
        <Field label="Longitud"><input className="input" required type="number" step="any" min="-180" max="180" value={form.lng} onChange={set('lng')} /></Field>
        <Field label="Dirección" className="col-span-2"><input className="input" value={form.address} onChange={set('address')} /></Field>
        <Field label="Teléfono"><input className="input" value={form.phone} onChange={set('phone')} /></Field>
        <Field label="Sitio web"><input className="input" type="url" value={form.website} onChange={set('website')} /></Field>
        <Field label="Descripción" className="col-span-2"><textarea className="input" rows={3} value={form.description} onChange={set('description')} /></Field>
      </div>
      {error && <p role="alert" className="rounded-xl bg-danger/10 p-3 text-[15px] text-danger">{error}</p>}
      {msg && <p role="status" className="rounded-xl bg-success/10 p-3 text-[15px] text-success">{msg}</p>}
      <button className="btn-primary w-full" disabled={busy}>{busy ? 'Guardando…' : 'Guardar lugar'}</button>
    </form>
  )
}

function Field({ label, className = '', children }) {
  return <label className={className}><span className="field-label">{label}</span>{children}</label>
}
