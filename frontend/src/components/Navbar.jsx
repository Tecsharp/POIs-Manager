import { Link, NavLink } from 'react-router-dom'
import { useState } from 'react'
import { Menu, X, Sun, Moon, Monitor, LogOut, MapPinPlus } from 'lucide-react'
import { useAuth } from '../lib/auth'
import { useTheme } from '../lib/theme'

const links = [
  { to: '/explorar', label: 'Explorar' },
  { to: '/ruta', label: 'Planear ruta' },
  { to: '/docs', label: 'API' },
]

const THEMES = [
  { id: 'light', label: 'Claro', icon: Sun },
  { id: 'dark', label: 'Oscuro', icon: Moon },
  { id: 'system', label: 'Automático', icon: Monitor },
]

export function Logo() {
  return (
    <span className="flex items-center gap-2">
      <svg viewBox="0 0 32 32" className="size-7" aria-hidden>
        <defs>
          <linearGradient id="rutea-g" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stopColor="var(--accent)" />
            <stop offset="1" stopColor="var(--brand-2)" />
          </linearGradient>
        </defs>
        <rect width="32" height="32" rx="9" fill="url(#rutea-g)" />
        <path d="M16 7c-4.1 0-7.2 3.1-7.2 6.9 0 5.1 7.2 11.1 7.2 11.1s7.2-6 7.2-11.1C23.2 10.1 20.1 7 16 7zm0 9.3a2.4 2.4 0 1 1 0-4.8 2.4 2.4 0 0 1 0 4.8z" fill="#fff" />
      </svg>
      <span className="text-[17px] font-bold tracking-tight">Rutea</span>
    </span>
  )
}

function ThemeSwitch() {
  const [mode, setMode] = useTheme()
  return (
    <div role="radiogroup" aria-label="Apariencia" className="flex rounded-full bg-fill p-0.5">
      {THEMES.map(({ id, label, icon: Icon }) => (
        <button key={id} role="radio" aria-checked={mode === id} aria-label={label} title={label} onClick={() => setMode(id)}
          className={`grid size-7 cursor-pointer place-items-center rounded-full transition-colors duration-200 ${mode === id ? 'bg-surface text-accent shadow-sm' : 'text-label-2 hover:text-label'}`}>
          <Icon className="size-3.5" />
        </button>
      ))}
    </div>
  )
}

export default function Navbar() {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const linkCls = ({ isActive }) =>
    `text-[13px] font-medium transition-colors duration-200 ${isActive ? 'text-label' : 'text-label-2 hover:text-label'}`

  return (
    <header className="sticky top-0 z-[1000] border-b border-sep bg-canvas">
      <nav className="mx-auto flex h-14 max-w-[1100px] items-center justify-between px-5">
        <Link to="/" onClick={() => setOpen(false)} aria-label="Rutea, inicio"><Logo /></Link>

        <div className="hidden items-center gap-8 md:flex">
          {links.map((l) => <NavLink key={l.to} to={l.to} className={linkCls}>{l.label}</NavLink>)}
          {user?.role === 'ADMIN' && <NavLink to="/admin" className={linkCls}>Admin</NavLink>}
          {user && <NavLink to="/cuenta" className={linkCls}>Mis API keys</NavLink>}
        </div>

        <div className="hidden items-center gap-3 md:flex">
          <ThemeSwitch />
          {user ? (
            <>
              <Link to="/agregar" className="btn-primary btn-sm"><MapPinPlus className="size-3.5" aria-hidden />Agregar lugar</Link>
              <button onClick={logout} className="grid size-8 cursor-pointer place-items-center rounded-full text-label-2 hover:bg-fill hover:text-label" aria-label="Cerrar sesión" title="Cerrar sesión">
                <LogOut className="size-4" />
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-[13px] font-medium text-label-2 hover:text-label">Entrar</Link>
              <Link to="/registro" className="btn-primary btn-sm">Crear cuenta</Link>
            </>
          )}
        </div>

        <button className="grid size-10 cursor-pointer place-items-center md:hidden" onClick={() => setOpen(!open)} aria-label="Menú" aria-expanded={open}>
          {open ? <X className="size-5" /> : <Menu className="size-5" />}
        </button>
      </nav>

      {open && (
        <div className="border-t border-sep bg-canvas px-5 pb-6 pt-2 md:hidden">
          <div className="divide-y divide-sep" onClick={() => setOpen(false)}>
            {links.map((l) => <NavLink key={l.to} to={l.to} className="block py-3 text-[17px] font-medium">{l.label}</NavLink>)}
            {user?.role === 'ADMIN' && <NavLink to="/admin" className="block py-3 text-[17px] font-medium">Admin</NavLink>}
            {user ? (
              <>
                <NavLink to="/agregar" className="block py-3 text-[17px] font-medium text-accent">Agregar lugar</NavLink>
                <NavLink to="/cuenta" className="block py-3 text-[17px] font-medium">Mis API keys</NavLink>
                <button onClick={logout} className="block w-full py-3 text-left text-[17px] text-danger">Cerrar sesión</button>
              </>
            ) : (
              <>
                <NavLink to="/login" className="block py-3 text-[17px] font-medium">Entrar</NavLink>
                <NavLink to="/registro" className="block py-3 text-[17px] font-medium text-accent">Crear cuenta</NavLink>
              </>
            )}
          </div>
          <div className="mt-4 flex items-center justify-between">
            <span className="text-[13px] text-label-2">Apariencia</span>
            <ThemeSwitch />
          </div>
        </div>
      )}
    </header>
  )
}
