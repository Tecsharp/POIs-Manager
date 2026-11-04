import { Link } from 'react-router-dom'
import { ChevronRight, Route, SlidersHorizontal, Sparkles, Check } from 'lucide-react'
import CodeBlock from '../components/CodeBlock'
import IconTile from '../components/IconTile'
import { DESTINATIONS } from '../lib/poiTypes'
import { apiBaseUrl } from '../lib/api'

const STOPS = [
  { km: 0, name: 'Zócalo de la Ciudad de México', type: 'SPOT' },
  { km: 128, name: 'Catedral de Puebla', type: 'CHURCH' },
  { km: 232, name: 'Gasolinera Autopista 150D', type: 'GAS_STATION' },
  { km: 251, name: 'Mirador Cumbres de Maltrata', type: 'PHOTO' },
  { km: 314, name: 'Cascada de Texolo', type: 'NATURAL_PARK' },
  { km: 398, name: 'Gran Café del Portal', type: 'CAFE' },
]

const FEATURES = [
  { icon: Route, title: 'Marca tu camino.', text: 'Toca el mapa para poner paradas o empieza con una ruta de ejemplo.' },
  { icon: SlidersHorizontal, title: 'Elige cuánto desviarte.', text: 'De 1 a 50 km a cada lado de la carretera. Tú decides.' },
  { icon: Sparkles, title: 'Descubre en orden.', text: 'Miradores, comida y gasolina, ordenados por cercanía a tu ruta.' },
]

export default function Home() {
  return (
    <>
      <section className="px-5 pb-16 pt-20 text-center sm:pt-28">
        <p className="text-[19px] font-semibold text-label-2">Rutea</p>
        <h1 className="mx-auto mt-2 max-w-3xl text-[44px] leading-[1.06] sm:text-[64px]">
          Todo lo que vale la pena, a lo largo de tu camino.
        </h1>
        <p className="mx-auto mt-5 max-w-xl text-[19px] leading-relaxed text-label-2 sm:text-[21px]">
          Miradores, cascadas, fondas y gasolineras a pocos kilómetros de tu ruta. Gratis y sin registrarte.
        </p>
        <div className="mt-8 flex flex-wrap items-center justify-center gap-x-6 gap-y-3">
          <Link to="/ruta" className="btn-primary">Planear una ruta</Link>
          <Link to="/explorar" className="inline-flex items-center text-[17px] text-accent hover:underline">
            Explorar el mapa <ChevronRight className="size-4" aria-hidden />
          </Link>
        </div>
      </section>

      {/* Vista previa de ruta, como una tarjeta de Mapas */}
      <section className="px-5 pb-24">
        <div className="card mx-auto max-w-2xl overflow-hidden">
          <div className="flex items-baseline justify-between px-5 pb-3 pt-5">
            <div>
              <p className="eyebrow">Ruta de ejemplo</p>
              <p className="mt-0.5 text-[22px] font-semibold tracking-tight">Ciudad de México a Veracruz</p>
            </div>
            <p className="tabular text-[15px] text-label-2">402 km</p>
          </div>
          <ol className="divide-y divide-sep border-t border-sep">
            {STOPS.map((s) => (
              <li key={s.name} className="flex items-center gap-3 px-5 py-3">
                <IconTile type={s.type} size={32} />
                <span className="min-w-0 flex-1 truncate text-[15px]">{s.name}</span>
                <span className="tabular shrink-0 text-[13px] text-label-2">km {s.km}</span>
              </li>
            ))}
          </ol>
          <Link to="/ruta?preset=cdmx-veracruz" className="flex items-center justify-center gap-1 border-t border-sep py-3.5 text-[15px] text-accent hover:bg-fill/50">
            Ver en el mapa <ChevronRight className="size-4" aria-hidden />
          </Link>
        </div>
      </section>

      <section className="bg-surface px-5 py-24">
        <div className="mx-auto max-w-[1100px]">
          <h2 className="max-w-2xl text-[40px] leading-tight sm:text-[48px]">No buscas un punto. Buscas un corredor.</h2>
          <div className="mt-12 grid gap-10 md:grid-cols-3">
            {FEATURES.map(({ icon: Icon, title, text }) => (
              <div key={title}>
                <Icon className="size-7 text-accent" strokeWidth={1.8} aria-hidden />
                <p className="mt-4 text-[19px] leading-snug"><span className="font-semibold">{title}</span> <span className="text-label-2">{text}</span></p>
              </div>
            ))}
          </div>
          <div className="mt-16">
            <p className="eyebrow">Destinos populares</p>
            <div className="mt-3 flex flex-wrap gap-2">
              {DESTINATIONS.map((d) => (
                <Link key={d.name} className="capsule" to={`/explorar?lat=${d.lat}&lng=${d.lng}&name=${encodeURIComponent(d.name)}`}>{d.name}</Link>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="px-5 py-24">
        <div className="mx-auto grid max-w-[1100px] gap-12 lg:grid-cols-2 lg:items-center">
          <div>
            <p className="eyebrow">Para desarrolladores</p>
            <h2 className="mt-2 text-[40px] leading-tight">La misma información, por API.</h2>
            <p className="mt-4 text-[19px] text-label-2">Pruébala sin registro. Cuando la integres en tu app, crea una cuenta y obtén una API key con cuota propia.</p>
            <div className="mt-8 grid gap-4 sm:grid-cols-2">
              <Plan title="Sin registro" items={['60 peticiones por minuto', 'Todos los endpoints de consulta']} />
              <Plan title="Con API key" items={['1,000 peticiones al día por key', 'Hasta 3 keys por cuenta', 'Uso diario y revocación']} />
            </div>
            <div className="mt-8 flex flex-wrap items-center gap-6">
              <Link to="/registro" className="btn-primary">Crear cuenta gratis</Link>
              <Link to="/docs" className="inline-flex items-center text-[17px] text-accent hover:underline">Documentación <ChevronRight className="size-4" aria-hidden /></Link>
            </div>
          </div>
          <CodeBlock label="Terminal" code={`curl -X POST ${apiBaseUrl()}/pois/route \\
  -H "X-API-Key: tr_tu_api_key" \\
  -H "Content-Type: application/json" \\
  -d '{
    "waypoints": [
      { "lat": 19.4326, "lng": -99.1332 },
      { "lat": 19.1903, "lng": -96.1533 }
    ],
    "buffer_km": 10,
    "types": ["spot", "gas_station"]
  }'`} />
        </div>
      </section>
    </>
  )
}

function Plan({ title, items }) {
  return (
    <div className="card p-5">
      <p className="text-[17px] font-semibold">{title}</p>
      <p className="text-[13px] text-label-2">Gratis</p>
      <ul className="mt-4 space-y-2 text-[15px]">
        {items.map((i) => (
          <li key={i} className="flex gap-2"><Check className="mt-1 size-4 shrink-0 text-success" aria-hidden />{i}</li>
        ))}
      </ul>
    </div>
  )
}
