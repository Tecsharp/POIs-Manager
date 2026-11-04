import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Sparkles } from 'lucide-react'
import PoiForm from '../components/PoiForm'

export default function AddPoi() {
  const [count, setCount] = useState(0)

  return (
    <div className="mx-auto max-w-xl px-5 py-14">
      <p className="eyebrow">Comunidad</p>
      <h1 className="mt-1 text-[34px] leading-tight">Agrega un lugar</h1>
      <p className="mt-2 text-[15px] text-label-2">
        Comparte un mirador, una fonda, un taller de confianza o cualquier lugar que valga la desviación.
        Otros viajeros podrán verlo en el mapa y votarlo como útil.
      </p>
      <div className="mt-8">
        <PoiForm onCreated={() => setCount((c) => c + 1)} />
      </div>
      {count > 0 && (
        <p className="mt-4 flex items-center gap-1.5 text-[13px] text-label-2">
          <Sparkles className="size-3.5 text-accent" aria-hidden />
          Llevas {count} {count === 1 ? 'lugar agregado' : 'lugares agregados'} hoy.{' '}
          <Link to="/explorar" className="text-accent hover:underline">Verlo en el mapa</Link>
        </p>
      )}
    </div>
  )
}
