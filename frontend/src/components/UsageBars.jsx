export default function UsageBars({ history, limit }) {
  const max = Math.max(limit ?? 0, ...history.map((h) => h.requests), 1)
  return (
    <div className="flex h-12 items-end gap-1" aria-label="Uso de los últimos 7 días">
      {history.map((h) => (
        <div key={h.date} className="group relative flex-1">
          <div className="w-full rounded-[3px] bg-accent/70 transition-colors group-hover:bg-accent"
            style={{ height: `${Math.max(2, (h.requests / max) * 48)}px` }} />
          <span className="pointer-events-none absolute bottom-full left-1/2 mb-1 hidden -translate-x-1/2 whitespace-nowrap rounded-md bg-label px-1.5 py-0.5 text-[11px] text-canvas group-hover:block">
            {h.date.slice(5)} · {h.requests}
          </span>
        </div>
      ))}
    </div>
  )
}
