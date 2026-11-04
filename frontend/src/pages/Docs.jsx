import { Link } from 'react-router-dom'
import CodeBlock from '../components/CodeBlock'
import { apiBaseUrl } from '../lib/api'
import { ALL_TYPES, typeLabel } from '../lib/poiTypes'

const ENDPOINTS = [
  ['POST', '/pois/route', 'Lugares dentro del corredor de una ruta, siguiendo la carretera real.'],
  ['GET', '/pois/nearby', 'Lugares en un radio alrededor de un punto.'],
  ['GET', '/pois/{id}', 'Detalle completo de un lugar (horario, teléfono, votos…).'],
  ['GET', '/pois/directions', 'Ruta por carretera entre dos puntos cualesquiera.'],
  ['GET', '/pois/types', 'Tipos de lugar disponibles.'],
  ['POST', '/pois', 'Da de alta un lugar. Requiere cuenta.'],
  ['POST', '/pois/{id}/vote', 'Vota un lugar como útil o no útil. Requiere cuenta.'],
]

export default function Docs() {
  const base = apiBaseUrl()
  return (
    <div className="mx-auto max-w-3xl px-5 py-16">
      <p className="eyebrow">Documentación</p>
      <h1 className="mt-1 text-[40px] leading-tight sm:text-[48px]">API de puntos de interés</h1>
      <p className="mt-4 text-[19px] text-label-2">
        Consulta lugares a lo largo de cualquier ruta. Puedes usarla sin registrarte, o con una API key para tener una cuota diaria propia.
      </p>

      <Section title="URL base">
        <CodeBlock label="base url" code={base} />
      </Section>

      <Section title="Acceso y límites">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="card p-5">
            <p className="font-semibold">Sin API key</p>
            <p className="mt-1 text-[15px] text-label-2">60 peticiones por minuto por dirección IP. Perfecto para probar.</p>
          </div>
          <div className="card p-5">
            <p className="font-semibold">Con API key</p>
            <p className="mt-1 text-[15px] text-label-2">1,000 peticiones al día por key (se reinicia a las 00:00 UTC). Hasta 3 keys activas. <Link to="/registro" className="text-accent hover:underline">Obtén la tuya</Link>.</p>
          </div>
        </div>
        <p className="mt-6 text-[15px] text-label-2">Envía tu key en cualquiera de estas cabeceras:</p>
        <div className="mt-3 grid gap-3">
          <CodeBlock label="cabecera recomendada" code="X-API-Key: tr_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" />
          <CodeBlock label="alternativa" code="Authorization: Bearer tr_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" />
        </div>
        <p className="mt-6 text-[15px] text-label-2">Cada respuesta incluye tu consumo:</p>
        <div className="card mt-3 overflow-hidden">
          <table className="w-full text-[15px]">
            <tbody className="divide-y divide-sep">
              {[
                ['X-RateLimit-Limit', 'Peticiones permitidas en la ventana actual'],
                ['X-RateLimit-Remaining', 'Peticiones restantes'],
                ['X-RateLimit-Reset', 'Epoch (segundos) en que se reinicia la cuota'],
                ['X-RateLimit-Scope', 'api-key o anonymous'],
              ].map(([h, d]) => (
                <tr key={h}><td className="px-4 py-2.5 font-mono text-[13px] text-label">{h}</td><td className="px-4 py-2.5 text-label-2">{d}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="mt-4 text-[15px] text-label-2">
          Si superas el límite recibirás <code className="font-mono">429</code> con la cabecera <code className="font-mono">Retry-After</code>. Una key inválida o revocada responde <code className="font-mono">401</code>.
        </p>
      </Section>

      <Section title="Endpoints">
        <div className="space-y-2">
          {ENDPOINTS.map(([m, p, d]) => (
            <div key={p} className="card flex flex-col gap-1 p-4 sm:flex-row sm:items-center sm:gap-4">
              <span className={`w-12 font-mono text-[13px] font-semibold ${m === 'GET' ? 'text-success' : 'text-accent'}`}>{m}</span>
              <code className="font-mono text-[15px]">{p}</code>
              <span className="text-[15px] text-label-2 sm:ml-auto sm:text-right">{d}</span>
            </div>
          ))}
        </div>
      </Section>

      <Section title="Lugares a lo largo de una ruta">
        <p className="mb-3 text-[15px] text-label-2">
          <b>waypoints</b> (2–500) definen la ruta. <b>buffer_km</b> (0.5–50, default 8) es el ancho del corredor a cada lado.
          Opcionales: <b>types</b>, <b>max_results</b> (1–200), <b>min_rating</b> (1–5), <b>verified_only</b>, <b>include_closed</b>.
        </p>
        <CodeBlock label="curl" code={`curl -X POST ${base}/pois/route \\
  -H "X-API-Key: $RUTEA_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{
    "waypoints": [
      { "lat": 19.4326, "lng": -99.1332 },
      { "lat": 19.1903, "lng": -96.1533 }
    ],
    "buffer_km": 10,
    "types": ["spot", "gas_station"]
  }'`} />
        <div className="mt-3">
          <CodeBlock label="respuesta" code={`{
  "total": 2,
  "route_length_km": 291.3,
  "buffer_km": 10.0,
  "routed": true,
  "route_geometry": [{ "lat": 19.4326, "lng": -99.1332 }, "…"],
  "pois": [
    { "id": "…", "name": "Zócalo de la Ciudad de México", "type": "spot",
      "lat": 19.4326, "lng": -99.1332, "rating": 4.8, "verified": false, "distance_km": 0.0,
      "upvotes": 12, "downvotes": 1, "created_by": null }
  ],
  "by_type": { "spot": 1, "gas_station": 1 }
}`} />
        </div>
        <p className="mt-3 text-[13px] text-label-2">
          La ruta se calcula sobre la carretera real (OSRM). Si no se puede enrutar, <b>routed</b> es <code className="font-mono">false</code> y
          <b> route_geometry</b> cae a línea recta entre waypoints, pero la búsqueda de lugares sigue funcionando igual.
        </p>
      </Section>

      <Section title="Lugares cerca de un punto">
        <p className="mb-3 text-[15px] text-label-2">
          Parámetros: <b>lat</b>, <b>lng</b>, <b>radius_km</b> (0.5–100, default 10), <b>type</b> (repetible), <b>limit</b> (1–200, default 50).
        </p>
        <CodeBlock label="javascript" code={`const res = await fetch(
  "${base}/pois/nearby?lat=17.0732&lng=-96.7266&radius_km=20&type=restaurant&type=cafe",
  { headers: { "X-API-Key": process.env.RUTEA_KEY } }
)
console.log(res.headers.get("X-RateLimit-Remaining"))
const { pois } = await res.json()`} />
      </Section>

      <Section title="Crear y votar lugares">
        <p className="mb-3 text-[15px] text-label-2">
          Dar de alta un lugar o votarlo requiere haber iniciado sesión — es una acción de cuenta, no de consulta,
          así que usa el token de tu sesión (<code className="font-mono">Authorization: Bearer &lt;token&gt;</code>), no tu API key.
          Los lugares que registra la comunidad quedan marcados con el nombre de quien los agregó; los de Rutea, verificados.
        </p>
        <CodeBlock label="curl" code={`curl -X POST ${base}/pois/\${POI_ID}/vote \\
  -H "Authorization: Bearer $TOKEN" \\
  -H "Content-Type: application/json" \\
  -d '{ "value": 1 }'`} />
      </Section>

      <Section title="Tipos de lugar">
        <div className="card grid gap-x-6 gap-y-2 p-5 sm:grid-cols-2">
          {ALL_TYPES.map((t) => (
            <span key={t} className="text-[13px]">
              <code className="font-mono text-label">{t.toLowerCase()}</code> <span className="text-label-2">{typeLabel(t)}</span>
            </span>
          ))}
        </div>
      </Section>
    </div>
  )
}

function Section({ title, children }) {
  return (
    <section className="mt-14">
      <h2 className="mb-4 text-[24px]">{title}</h2>
      {children}
    </section>
  )
}
