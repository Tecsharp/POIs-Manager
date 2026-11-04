import { Star, BadgeCheck } from 'lucide-react'
import { typeLabel } from '../lib/poiTypes'
import IconTile from './IconTile'

export default function PoiListItem({ poi, active, onClick }) {
  return (
    <button onClick={onClick} aria-current={active || undefined}
      className={`flex w-full cursor-pointer items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-colors duration-150 ${active ? 'bg-accent/10' : 'hover:bg-fill'}`}>
      <IconTile type={poi.type} size={36} />
      <span className="min-w-0 flex-1">
        <span className="flex items-center gap-1 text-[15px] font-medium leading-snug">
          <span className="truncate">{poi.name}</span>
          {poi.verified && <BadgeCheck className="size-4 shrink-0 text-accent" aria-label="Verificado" />}
        </span>
        <span className="flex items-center gap-1.5 text-[13px] text-label-2">
          <span className="truncate">{typeLabel(poi.type)}</span>
          {poi.rating != null && (
            <>
              <span aria-hidden>·</span>
              <span className="flex shrink-0 items-center gap-0.5"><Star className="size-3 fill-current text-warning" aria-hidden />{poi.rating.toFixed(1)}</span>
            </>
          )}
        </span>
      </span>
      {poi.distance_km != null && (
        <span className="tabular shrink-0 text-[13px] text-label-2">{poi.distance_km} km</span>
      )}
    </button>
  )
}
