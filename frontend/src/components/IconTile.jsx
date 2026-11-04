import { categoryOf } from '../lib/poiTypes'

/** Insignia de categoría: círculo con tinte suave del color de la categoría y el glifo en ese mismo color. */
export default function IconTile({ type, category, size = 36 }) {
  const cat = category ?? categoryOf(type)
  const Icon = cat.icon
  return (
    <span aria-hidden className="grid shrink-0 place-items-center rounded-full ring-1"
      style={{
        width: size, height: size, color: cat.color,
        background: `color-mix(in srgb, ${cat.color} 15%, var(--surface))`,
        '--tw-ring-color': `color-mix(in srgb, ${cat.color} 22%, transparent)`,
      }}>
      <Icon size={Math.round(size * 0.52)} strokeWidth={2} />
    </span>
  )
}
