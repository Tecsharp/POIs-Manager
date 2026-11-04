import { categoryOf } from '../lib/poiTypes'

/** Ícono de categoría al estilo de Ajustes de iOS: glifo blanco sobre cuadro redondeado de color. */
export default function IconTile({ type, category, size = 36 }) {
  const cat = category ?? categoryOf(type)
  const Icon = cat.icon
  return (
    <span aria-hidden className="grid shrink-0 place-items-center text-white"
      style={{ width: size, height: size, borderRadius: size * 0.26, background: cat.color }}>
      <Icon size={Math.round(size * 0.55)} strokeWidth={2.1} />
    </span>
  )
}
