import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useEffect } from 'react'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import Home from './pages/Home'
import Explore from './pages/Explore'
import RoutePlanner from './pages/RoutePlanner'
import Docs from './pages/Docs'
import Login from './pages/Login'
import Register from './pages/Register'
import Account from './pages/Account'
import Admin from './pages/Admin'
import AddPoi from './pages/AddPoi'
import { useAuth } from './lib/auth'

function RequireAuth({ children, admin = false }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <div className="grid min-h-[60vh] place-items-center text-label-2">Cargando…</div>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (admin && user.role !== 'ADMIN') return <Navigate to="/cuenta" replace />
  return children
}

export default function App() {
  const { pathname } = useLocation()
  useEffect(() => { window.scrollTo(0, 0) }, [pathname])
  const fullBleed = pathname === '/explorar' || pathname === '/ruta'

  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <main className="flex-1">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/explorar" element={<Explore />} />
          <Route path="/ruta" element={<RoutePlanner />} />
          <Route path="/docs" element={<Docs />} />
          <Route path="/login" element={<Login />} />
          <Route path="/registro" element={<Register />} />
          <Route path="/cuenta" element={<RequireAuth><Account /></RequireAuth>} />
          <Route path="/agregar" element={<RequireAuth><AddPoi /></RequireAuth>} />
          <Route path="/admin" element={<RequireAuth admin><Admin /></RequireAuth>} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
      {!fullBleed && <Footer />}
    </div>
  )
}

function NotFound() {
  return (
    <div className="mx-auto max-w-xl px-6 py-32 text-center">
      <p className="text-[17px] font-semibold text-label-2">Error 404</p>
      <h1 className="mt-2 text-[40px] leading-tight">Este camino no lleva a ningún lado.</h1>
      <Link to="/" className="btn-primary mt-8">Volver al inicio</Link>
    </div>
  )
}
