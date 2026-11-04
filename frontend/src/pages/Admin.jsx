import { useEffect, useState } from 'react'
import { Users, KeyRound, MapPin, Activity, Trash2 } from 'lucide-react'
import { api } from '../lib/api'
import { useAuth } from '../lib/auth'
import { typeLabel } from '../lib/poiTypes'
import PoiForm from '../components/PoiForm'

const TABS = [
  { id: 'keys', label: 'API keys' },
  { id: 'users', label: 'Usuarios' },
  { id: 'pois', label: 'Lugares' },
]

export default function Admin() {
  const [tab, setTab] = useState('keys')
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')
  const loadStats = () => api('/admin/stats').then(setStats).catch((e) => setError(e.message))
  useEffect(() => { loadStats() }, [])

  return (
    <div className="mx-auto max-w-[1100px] px-5 py-12">
      <h1 className="text-[34px] leading-tight">Panel de administración</h1>
      {error && <p role="alert" className="mt-4 rounded-xl bg-danger/10 p-3 text-[15px] text-danger">{error}</p>}
      <div className="mt-8 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard icon={Users} label="Usuarios" value={stats?.users} />
        <StatCard icon={KeyRound} label="Keys activas" value={stats?.active_keys} />
        <StatCard icon={MapPin} label="Lugares" value={stats?.pois} />
        <StatCard icon={Activity} label="Peticiones hoy (keys)" value={stats?.requests_today} />
      </div>
      <div className="mt-10 flex gap-6 border-b border-sep" role="tablist" aria-label="Secciones">
        {TABS.map((t) => (
          <button key={t.id} role="tab" aria-selected={tab === t.id} onClick={() => setTab(t.id)}
            className={`-mb-px cursor-pointer border-b-2 pb-3 text-[14px] font-semibold transition-colors duration-200 ${tab === t.id ? 'border-accent text-label' : 'border-transparent text-label-2 hover:text-label'}`}>
            {t.label}
          </button>
        ))}
      </div>
      <div className="mt-6">
        {tab === 'keys' && <KeysTab onChange={loadStats} />}
        {tab === 'users' && <UsersTab onChange={loadStats} />}
        {tab === 'pois' && <PoisTab onChange={loadStats} />}
      </div>
    </div>
  )
}

function StatCard({ icon: Icon, label, value }) {
  return (
    <div className="card p-5">
      <Icon className="size-5 text-accent" aria-hidden />
      <p className="mt-3 text-3xl text-label">{value ?? '—'}</p>
      <p className="text-[13px] text-label-2">{label}</p>
    </div>
  )
}

function useList(path) {
  const [items, setItems] = useState(null)
  const [error, setError] = useState('')
  const reload = () => api(path).then(setItems).catch((e) => setError(e.message))
  useEffect(() => { reload() }, [path])
  return { items, error, setError, reload }
}

function Table({ head, children }) {
  return (
    <div className="card overflow-x-auto">
      <table className="w-full text-left text-[15px]">
        <thead className="border-b border-sep text-[12px] text-label-2">
          <tr>{head.map((h) => <th key={h} className="px-4 py-2.5 font-medium">{h}</th>)}</tr>
        </thead>
        <tbody className="divide-y divide-sep">{children}</tbody>
      </table>
    </div>
  )
}

function KeysTab({ onChange }) {
  const { items, error, setError, reload } = useList('/admin/keys')
  const patch = async (k, body) => {
    try {
      await api(`/admin/keys/${k.id}`, { method: 'PATCH', body })
      reload()
      onChange()
    } catch (e) { setError(e.message) }
  }
  const editLimit = (k) => {
    const v = prompt(`Nuevo límite diario para "${k.name}"`, k.daily_limit)
    if (v !== null && Number.isInteger(+v)) patch(k, { daily_limit: +v })
  }
  return (
    <>
      {error && <p className="mb-3 rounded-xl bg-danger/10 p-3 text-[15px] text-danger">{error}</p>}
      <Table head={['Key', 'Dueño', 'Hoy', 'Último uso', 'Estado', '']}>
        {items?.map((k) => (
          <tr key={k.id}>
            <td className="px-4 py-3"><p className="font-semibold">{k.name}</p><code className="font-mono text-[12px] text-label-2">{k.prefix}…</code></td>
            <td className="px-4 py-3 text-label-2">{k.owner_email}</td>
            <td className="px-4 py-3">
              <button onClick={() => editLimit(k)} className="tabular cursor-pointer text-accent hover:underline" title="Cambiar límite diario">
                {k.used_today} / {k.daily_limit}
              </button>
            </td>
            <td className="px-4 py-3 text-[12px] text-label-2">{k.last_used_at ? new Date(k.last_used_at).toLocaleString('es-MX') : 'Nunca'}</td>
            <td className="px-4 py-3"><Badge on={k.active} onLabel="Activa" offLabel="Revocada" /></td>
            <td className="px-4 py-3 text-right">
              <button onClick={() => patch(k, { active: !k.active })} className={k.active ? 'btn-danger btn-sm' : 'btn-secondary btn-sm'}>
                {k.active ? 'Revocar' : 'Reactivar'}
              </button>
            </td>
          </tr>
        ))}
      </Table>
      {items?.length === 0 && <p className="p-6 text-center text-[15px] text-label-2">Nadie ha creado keys todavía.</p>}
    </>
  )
}

function UsersTab({ onChange }) {
  const { user: me } = useAuth()
  const { items, error, setError, reload } = useList('/admin/users')
  const toggle = async (u) => {
    if (u.enabled && !confirm(`¿Deshabilitar a ${u.email}? Sus API keys dejarán de funcionar.`)) return
    try {
      await api(`/admin/users/${u.id}`, { method: 'PATCH', body: { enabled: !u.enabled } })
      reload()
      onChange()
    } catch (e) { setError(e.message) }
  }
  return (
    <>
      {error && <p className="mb-3 rounded-xl bg-danger/10 p-3 text-[15px] text-danger">{error}</p>}
      <Table head={['Usuario', 'Rol', 'Keys activas', 'Alta', 'Estado', '']}>
        {items?.map(({ user: u, active_keys }) => (
          <tr key={u.id}>
            <td className="px-4 py-3"><p className="font-semibold">{u.name}</p><p className="text-[12px] text-label-2">{u.email}</p></td>
            <td className="px-4 py-3 text-[13px] text-label-2">{u.role === 'ADMIN' ? 'Administrador' : 'Usuario'}</td>
            <td className="px-4 py-3">{active_keys}</td>
            <td className="px-4 py-3 text-[12px] text-label-2">{new Date(u.created_at).toLocaleDateString('es-MX')}</td>
            <td className="px-4 py-3"><Badge on={u.enabled} onLabel="Activo" offLabel="Deshabilitado" /></td>
            <td className="px-4 py-3 text-right">
              {u.id !== me.id && (
                <button onClick={() => toggle(u)} className={u.enabled ? 'btn-danger btn-sm' : 'btn-secondary btn-sm'}>
                  {u.enabled ? 'Deshabilitar' : 'Habilitar'}
                </button>
              )}
            </td>
          </tr>
        ))}
      </Table>
    </>
  )
}

function PoisTab({ onChange }) {
  const { items, error, setError, reload } = useList('/admin/pois')

  const remove = async (p) => {
    if (!confirm(`¿Eliminar "${p.name}"? Esta acción no se puede deshacer.`)) return
    try {
      await api(`/pois/${p.id}`, { method: 'DELETE' })
      reload()
      onChange()
    } catch (err) { setError(err.message) }
  }

  return (
    <div className="grid gap-8 lg:grid-cols-[1fr_1.2fr]">
      <div>
        <h2 className="mb-4 text-[20px]">Agregar lugar</h2>
        <PoiForm onCreated={() => { reload(); onChange() }} />
      </div>

      <div>
        <h2 className="mb-4 text-[20px]">Últimos agregados</h2>
        {error && <p role="alert" className="mb-3 rounded-xl bg-danger/10 p-3 text-[15px] text-danger">{error}</p>}
        <div className="max-h-[720px] space-y-2 overflow-y-auto pr-1">
          {items?.map((p) => (
            <div key={p.id} className="flex items-center justify-between gap-3 rounded-xl bg-surface p-3 ring-1 ring-sep">
              <div className="min-w-0">
                <p className="flex items-center gap-1.5 truncate font-semibold">
                  {p.name}
                  {p.verified ? (
                    <span className="shrink-0 rounded-full bg-accent/15 px-1.5 py-0.5 text-[10px] font-bold text-accent">Rutea</span>
                  ) : (
                    <span className="shrink-0 rounded-full bg-fill px-1.5 py-0.5 text-[10px] font-bold text-label-2">Comunidad</span>
                  )}
                </p>
                <p className="text-[12px] text-label-2">
                  {typeLabel(p.type)} · {p.lat.toFixed(4)}, {p.lng.toFixed(4)}
                  {p.created_by && ` · ${p.created_by}`}
                </p>
              </div>
              <button onClick={() => remove(p)} className="grid size-9 shrink-0 cursor-pointer place-items-center rounded-full text-danger hover:bg-fill" aria-label={`Eliminar ${p.name}`}>
                <Trash2 className="size-4" />
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function Badge({ on, onLabel, offLabel }) {
  return (
    <span className="inline-flex items-center gap-1.5 whitespace-nowrap text-[13px] text-label-2">
      <span aria-hidden className={`size-2 rounded-full ${on ? 'bg-success' : 'bg-label-3'}`} />
      {on ? onLabel : offLabel}
    </span>
  )
}
