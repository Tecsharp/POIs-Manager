import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/auth'

export default function AuthForm({ mode }) {
  const isRegister = mode === 'register'
  const { login, register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const user = isRegister ? await register(form.name, form.email, form.password) : await login(form.email, form.password)
      navigate(location.state?.from ?? (user.role === 'ADMIN' ? '/admin' : '/cuenta'), { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid min-h-[calc(100dvh-3rem)] place-items-center px-5 py-16">
      <div className="w-full max-w-[380px]">
        <div className="text-center">
          <h1 className="text-[32px] leading-tight">{isRegister ? 'Crea tu cuenta' : 'Inicia sesión'}</h1>
          <p className="mt-2 text-[15px] text-label-2">
            {isRegister
              ? 'Solo la necesitas para obtener API keys. Explorar y planear rutas siempre es gratis.'
              : 'Administra tus API keys y revisa su uso.'}
          </p>
        </div>
        <form onSubmit={submit} className="card mt-8 space-y-4 p-6" noValidate={false}>
          {isRegister && (
            <div>
              <label className="field-label" htmlFor="name">Nombre</label>
              <input id="name" className="input" required maxLength={80} autoComplete="name" value={form.name} onChange={set('name')} />
            </div>
          )}
          <div>
            <label className="field-label" htmlFor="email">Correo</label>
            <input id="email" type="email" className="input" required autoComplete="email" value={form.email} onChange={set('email')} />
          </div>
          <div>
            <label className="field-label" htmlFor="password">Contraseña</label>
            <input id="password" type="password" className="input" required minLength={isRegister ? 8 : undefined} maxLength={72}
              aria-describedby={isRegister ? 'pw-help' : undefined}
              autoComplete={isRegister ? 'new-password' : 'current-password'} value={form.password} onChange={set('password')} />
            {isRegister && <p id="pw-help" className="mt-1.5 text-[12px] text-label-2">Mínimo 8 caracteres.</p>}
          </div>
          {error && <p role="alert" className="rounded-xl bg-danger/10 p-3 text-[15px] text-danger">{error}</p>}
          <button className="btn-primary w-full" disabled={busy}>
            {busy ? 'Un momento…' : isRegister ? 'Crear cuenta' : 'Continuar'}
          </button>
        </form>
        <p className="mt-6 text-center text-[15px] text-label-2">
          {isRegister ? '¿Ya tienes cuenta? ' : '¿No tienes cuenta? '}
          <Link to={isRegister ? '/login' : '/registro'} className="text-accent hover:underline">
            {isRegister ? 'Inicia sesión' : 'Crea una gratis'}
          </Link>
        </p>
      </div>
    </div>
  )
}
