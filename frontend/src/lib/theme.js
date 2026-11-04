import { useEffect, useState } from 'react'

const media = () => window.matchMedia('(prefers-color-scheme: dark)')

function apply(mode) {
  const dark = mode === 'dark' || (mode === 'system' && media().matches)
  document.documentElement.classList.toggle('dark', dark)
}

/** Tema: 'system' | 'light' | 'dark'. Persistido en localStorage; el script de index.html lo aplica antes de pintar. */
export function useTheme() {
  const [mode, setMode] = useState(() => localStorage.getItem('theme') || 'system')

  useEffect(() => {
    localStorage.setItem('theme', mode)
    apply(mode)
    if (mode !== 'system') return undefined
    const mq = media()
    const onChange = () => apply('system')
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [mode])

  return [mode, setMode]
}
