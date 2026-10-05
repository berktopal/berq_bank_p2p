import { useCallback, useEffect, useSyncExternalStore } from 'react'

export type ThemePreference = 'light' | 'dark' | 'system'
const KEY = 'berq.theme'
const media = () => window.matchMedia('(prefers-color-scheme: dark)')

function readPreference(): ThemePreference {
  try {
    const v = localStorage.getItem(KEY)
    return v === 'light' || v === 'dark' ? v : 'system'
  } catch {
    return 'system'
  }
}

function apply(pref: ThemePreference) {
  const dark = pref === 'dark' || (pref === 'system' && media().matches)
  document.documentElement.dataset.theme = dark ? 'dark' : 'light'
}

const listeners = new Set<() => void>()
function subscribe(cb: () => void) {
  listeners.add(cb)
  return () => listeners.delete(cb)
}

/** Tema tercihi (açık/koyu/sistem). Sistem seçiliyken işletim sistemi değişikliği anında yansır. */
export function useTheme() {
  const preference = useSyncExternalStore(subscribe, readPreference, () => 'system' as const)
  const resolved = useSyncExternalStore<'light' | 'dark'>(
    subscribe,
    () => (document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light'),
    () => 'light' as const,
  )

  useEffect(() => {
    if (preference !== 'system') return
    const mq = media()
    const onChange = () => {
      apply('system')
      listeners.forEach((l) => l())
    }
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [preference])

  const setTheme = useCallback((pref: ThemePreference) => {
    try {
      if (pref === 'system') localStorage.removeItem(KEY)
      else localStorage.setItem(KEY, pref)
    } catch {
      // tercih kalıcı olmaz ama tema yine değişir
    }
    apply(pref)
    listeners.forEach((l) => l())
  }, [])

  const toggle = useCallback(() => setTheme(resolved === 'dark' ? 'light' : 'dark'), [resolved, setTheme])

  return { preference, resolved, setTheme, toggle }
}
