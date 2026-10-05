import { Languages, Moon, Sun } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useTheme } from '@/hooks/useTheme'
import { setLanguage } from '@/i18n'
import { cn } from '@/lib/cn'

const iconButton =
  'text-muted hover:text-text hover:bg-surface-2 inline-flex h-10 items-center justify-center gap-1.5 rounded-xl px-2.5 text-sm font-semibold transition'

export function ThemeToggle({ className }: { className?: string }) {
  const { t } = useTranslation()
  const { resolved, toggle } = useTheme()
  return (
    <button
      type="button"
      onClick={toggle}
      className={cn(iconButton, 'w-10 px-0', className)}
      aria-label={resolved === 'dark' ? t('theme.light') : t('theme.dark')}
      title={t('theme.toggle')}
    >
      {resolved === 'dark' ? <Sun className="size-5" /> : <Moon className="size-5" />}
    </button>
  )
}

export function LanguageToggle({ className }: { className?: string }) {
  const { t, i18n } = useTranslation()
  const next = i18n.language === 'en' ? 'tr' : 'en'
  return (
    <button
      type="button"
      onClick={() => setLanguage(next)}
      className={cn(iconButton, className)}
      aria-label={`${t('language.toggle')}: ${t(`language.${next}`)}`}
      title={t('language.toggle')}
    >
      <Languages className="size-5" />
      <span aria-hidden>{next.toUpperCase()}</span>
    </button>
  )
}
