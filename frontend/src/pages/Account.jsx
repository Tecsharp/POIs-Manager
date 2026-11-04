import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { KeyRound, Plus, Copy, Check, AlertTriangle, Ban } from 'lucide-react'
import { api, apiBaseUrl } from '../lib/api'
import { useAuth } from '../lib/auth'
import CodeBlock, { useCopy } from '../components/CodeBlock'
import UsageBars from '../components/UsageBars'

const fmt = (d) => (d ? new Date(d).toLocaleString('es-MX', { dateStyle: 'medium', timeStyle: 'short' }) : 'Nunca')

export default function Account() {
  const { user } = useAuth()
  const [keys, setKeys] = useState(null)
  const [name, setName] = useState('')
  const [created, setCreated] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = () => api('/keys').then(setKeys).catch((e) => setError(e.message))
  useEffect(() => { load() }, [])

  const create = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      setCreated(await api('/keys', { method: 'POST', body: { name } }))
      setName('')
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const revoke = async (k) => {
    if (!confirm(`¿Revocar "${k.name}"? Las apps que la usen dejarán de funcionar de inmediato. No se puede deshacer.`)) return
    try {
      await api(`/keys/${k.id}`, { method: 'DELETE' })
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const active = keys?.filter((k) => k.active) ?? []
  const revoked = keys?.filter((k) => !k.active) ?? []

  return (
    <div className="mx-auto max-w-3xl px-5 py-14">
      <p className="text-[15px] text-label-2">Hola, {user.name}</p>
      <h1 className="mt-1 text-[34px] leading-tight">Mis API keys</h1>
      <p className="mt-2 text-label-2">Cada key tiene su propia cuota diaria. Guárdalas como contraseñas: no las publiques en repositorios ni en código del navegador.</p>

      {created && (
        <div className="card mt-8 p-6 ring-2 ring-warning/60" role="status">
          <p className="flex items-center gap-2 text-[17px] font-semibold"><AlertTriangle className="size-5 text-warning" aria-hidden />Copia tu key ahora. No volverás a verla.</p>
          <p className="mt-1 text-[15px] text-label-2">Solo guardamos una huella (hash) de la key. Si la pierdes, revócala y crea otra.</p>
          <KeyReveal value={created.key} />
          <button onClick={() => setCreated(null)} className="mt-4 cursor-pointer text-[15px] text-accent hover:underline">Ya la guardé</button>
        </div>
      )}

      <form onSubmit={create} className="card mt-8 flex flex-col gap-3 p-5 sm:flex-row sm:items-end">
        <div className="flex-1">
          <label className="field-label" htmlFor="keyname">Nombre de la nueva key</label>
          <input id="keyname" className="input" placeholder="p. ej. App de viajes – producción" required maxLength={60}
            value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <button className="btn-primary" disabled={busy || !name.trim()}><Plus className="size-4" />Crear key</button>
      </form>
      {error && <p role="alert" className="mt-3 rounded-xl bg-danger/10 p-3 text-[15px] text-danger">{error}</p>}

      <h2 className="mt-12 text-[20px]">Activas <span className="text-label-3">({active.length})</span></h2>
      {keys === null && <p className="mt-4 text-[15px] text-label-2">Cargando…</p>}
      {keys && active.length === 0 && (
        <div className="mt-4 card p-10 text-center">
          <KeyRound className="mx-auto size-8 text-label-3" />
          <p className="mt-3 font-semibold">Aún no tienes keys activas</p>
          <p className="text-[15px] text-label-2">Ponle un nombre arriba y crea la primera.</p>
        </div>
      )}
      <div className="mt-4 space-y-4">
        {active.map((k) => (
          <div key={k.id} className="card p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="font-semibold">{k.name}</p>
                <code className="font-mono text-[15px] text-label-2">{k.prefix}…</code>
              </div>
              <button onClick={() => revoke(k)} className="btn-danger btn-sm"><Ban className="size-3.5" />Revocar</button>
            </div>
            <div className="mt-5 grid gap-6 sm:grid-cols-[1fr_200px]">
              <div>
                <div className="flex justify-between text-[13px] text-label-2">
                  <span>Uso de hoy</span><span className="text-label">{k.used_today.toLocaleString()} / {k.daily_limit.toLocaleString()}</span>
                </div>
                <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-fill">
                  <div className={`h-full rounded-full ${k.used_today >= k.daily_limit ? 'bg-danger' : 'bg-accent'}`}
                    style={{ width: `${Math.min(100, (k.used_today / k.daily_limit) * 100)}%` }} />
                </div>
                <p className="mt-3 text-[12px] text-label-2">Creada {fmt(k.created_at)} · Último uso: {fmt(k.last_used_at)}</p>
              </div>
              <div>
                <p className="mb-1 text-[13px] text-label-2">Últimos 7 días</p>
                <UsageBars history={k.history} limit={k.daily_limit} />
              </div>
            </div>
          </div>
        ))}
      </div>

      {revoked.length > 0 && (
        <>
          <h2 className="mt-12 text-[20px] text-label-2">Revocadas</h2>
          <ul className="mt-3 divide-y divide-sep rounded-2xl bg-surface ring-1 ring-sep">
            {revoked.map((k) => (
              <li key={k.id} className="flex justify-between px-4 py-3 text-[15px] text-label-2">
                <span><b>{k.name}</b> · <code className="font-mono">{k.prefix}…</code></span>
                <span>Revocada {fmt(k.revoked_at)}</span>
              </li>
            ))}
          </ul>
        </>
      )}

      <div className="mt-12">
        <h2 className="mb-3 text-[20px]">Pruébala</h2>
        <CodeBlock label="Terminal" code={`curl "${apiBaseUrl()}/pois/nearby?lat=19.43&lng=-99.13&radius_km=10" \\\n  -H "X-API-Key: tr_…"`} />
        <Link to="/docs" className="mt-4 inline-block text-[15px] text-accent hover:underline">Documentación completa</Link>
      </div>
    </div>
  )
}

function KeyReveal({ value }) {
  const [copied, copy] = useCopy()
  return (
    <div className="mt-4 flex items-center gap-2 rounded-xl bg-fill p-1.5 pl-4">
      <code className="flex-1 overflow-x-auto whitespace-nowrap font-mono text-[14px]">{value}</code>
      <button onClick={() => copy(value)} className="btn-primary !px-4 !py-2">
        {copied ? <><Check className="size-4" />Copiada</> : <><Copy className="size-4" />Copiar</>}
      </button>
    </div>
  )
}
