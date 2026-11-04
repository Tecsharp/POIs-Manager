import { useState } from 'react'
import { Check, Copy } from 'lucide-react'

export function useCopy() {
  const [copied, setCopied] = useState(false)
  const copy = async (text) => {
    try {
      await navigator.clipboard.writeText(text)
    } catch {
      // http sin contexto seguro: fallback clásico
      const ta = Object.assign(document.createElement('textarea'), { value: text })
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      ta.remove()
    }
    setCopied(true)
    setTimeout(() => setCopied(false), 1600)
  }
  return [copied, copy]
}

export default function CodeBlock({ code, label }) {
  const [copied, copy] = useCopy()
  return (
    <div className="overflow-hidden rounded-[14px] bg-[#1d1d1f] text-[#f5f5f7] ring-1 ring-sep dark:bg-[#141416]">
      <div className="flex items-center justify-between border-b border-white/10 px-4 py-2">
        <span className="text-[12px] font-medium text-white/50">{label}</span>
        <button onClick={() => copy(code)} className="flex cursor-pointer items-center gap-1 text-[12px] font-medium text-white/70 hover:text-white">
          {copied ? <><Check className="size-3.5" />Copiado</> : <><Copy className="size-3.5" />Copiar</>}
        </button>
      </div>
      <pre className="overflow-x-auto p-4 font-mono text-[13px] leading-relaxed"><code>{code}</code></pre>
    </div>
  )
}
