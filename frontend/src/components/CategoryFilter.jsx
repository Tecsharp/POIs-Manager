import { CATEGORIES } from '../lib/poiTypes'

export default function CategoryFilter({ selected, onChange }) {
  const toggle = (id) => onChange(selected.includes(id) ? selected.filter((s) => s !== id) : [...selected, id])
  return (
    <div className="no-scrollbar -mx-5 flex gap-2 overflow-x-auto px-5" role="group" aria-label="Filtrar por categoría">
      <button className="capsule" aria-pressed={selected.length === 0} onClick={() => onChange([])}>Todo</button>
      {CATEGORIES.map(({ id, label, icon: Icon, color }) => {
        const on = selected.includes(id)
        return (
          <button key={id} className="capsule" aria-pressed={on} onClick={() => toggle(id)}>
            <Icon className="size-3.5" style={on ? undefined : { color }} aria-hidden />
            {label}
          </button>
        )
      })}
    </div>
  )
}
