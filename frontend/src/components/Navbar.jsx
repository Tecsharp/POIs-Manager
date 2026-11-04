import { Link, NavLink } from 'react-router-dom'
import { useState } from 'react'
import { Menu, X, Sun, Moon, Monitor, LogOut } from 'lucide-react'
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
      <svg viewBox="0 0 32 32" className="size-6" aria-hidden>
        <rect width="32" height="32" rx="7.5" fill="var(--accent)" />
        <path d="M16 6.5c-4.1 0-7.5 3.2-7.5 7.2 0 5.4 7.5 11.8 7.5 11.8s7.5-6.4 7.5-11.8c0-4-3.4-7.2-7.5-7.2zm0 9.8a2.6 2.6 0 1 1 0-5.2 2.6 2.6 0 0 1 0 5.2z" fill="#fff" />
      </svg>
      <span className="text-[17px] font-semibold tracking-tight">Rutea</span>
    </span>
  )
}

function ThemeSwitch() {
  const [mode, setMode] = useTheme()
  return (
    <div role="radiogroup" aria-label="Apariencia" className="flex rounded-full bg-fill p-0.5">
      {THEMES.map(({ id, label, icon: Icon }) => (
        <button key={id} role="radio" aria-checked={mode === id} aria-label={label} title={label} onClick={() => setMode(id)}
          className={`grid size-7 cursor-pointer place-items-center rounded-full transition-colors duration-200 ${mode === id ? 'bg-surface text-label shadow-sm' : 'text-label-2 hover:text-label'}`}>
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
    `text-[13px] transition-colors duration-200 ${isActive ? 'text-label' : 'text-label-2 hover:text-label'}`

  return (
    <header className="sticky top-0 z-[1000] border-b border-sep bg-canvas/80 backdrop-blur-xl backdrop-saturate-150">
      <nav className="mx-auto flex h-12 max-w-[1100px] items-center justify-between px-5">
        <Link to="/" onClick={() => setOpen(false)} aria-label="Rutea, inicio"><Logo /></Link>

        <div className="hidden items-center gap-8 md:flex">
          {links.map((l) => <NavLink key={l.to} to={l.to} className={linkCls}>{l.label}</NavLink>)}
          {user?.role === 'ADMIN' && <NavLink to="/admin" className={linkCls}>Admin</NavLink>}
          {user && <NavLink to="/cuenta" className={linkCls}>Mis API keys</NavLink>}
        </div>

        <div className="hidden items-center gap-3 md:flex">
          <ThemeSwitch />
          {user ? (
            <button onClick={logout} className="grid size-8 cursor-pointer place-items-center rounded-full text-label-2 hover:bg-fill hover:text-label" aria-label="Cerrar sesión" title="Cerrar sesión">
              <LogOut className="size-4" />
            </button>
          ) : (
            <>
              <Link to="/login" className="text-[13px] text-label-2 hover:text-label">Entrar</Link>
              <Link to="/registro" className="btn-primary btn-sm">Obtener API key</Link>
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
                <NavLink to="/cuenta" className="block py-3 text-[17px] font-medium">Mis API keys</NavLink>
                <button onClick={logout} className="block w-full py-3 text-left text-[17px] text-danger">Cerrar sesión</button>
              </>
            ) : (
              <>
                <NavLink to="/login" className="block py-3 text-[17px] font-medium">Entrar</NavLink>
                <NavLink to="/registro" className="block py-3 text-[17px] font-medium text-accent">Obtener API key</NavLink>
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
