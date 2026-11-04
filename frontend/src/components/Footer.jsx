import { Link } from 'react-router-dom'

export default function Footer() {
  return (
    <footer className="bg-canvas text-[12px] text-label-2">
      <div className="mx-auto max-w-[1100px] border-t border-sep px-5 py-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p>Rutea · Lugares que valen la desviación.</p>
          <nav className="flex flex-wrap gap-x-5 gap-y-2">
            <Link to="/explorar" className="hover:text-label hover:underline">Explorar</Link>
            <Link to="/ruta" className="hover:text-label hover:underline">Planear ruta</Link>
            <Link to="/docs" className="hover:text-label hover:underline">Documentación de la API</Link>
            <Link to="/registro" className="hover:text-label hover:underline">Obtener API key</Link>
          </nav>
        </div>
        <p className="mt-3 text-label-3">Mapas © colaboradores de OpenStreetMap.</p>
      </div>
    </footer>
  )
}
