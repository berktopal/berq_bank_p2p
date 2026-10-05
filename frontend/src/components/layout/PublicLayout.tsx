import { useTranslation } from 'react-i18next'
import { Link, Outlet } from 'react-router'
import { ButtonLink } from '@/components/ui/Button'
import { useMe } from '@/features/auth/api'
import { Logo } from './Logo'
import { LanguageToggle, ThemeToggle } from './Toggles'

export function PublicLayout() {
  const { t } = useTranslation()
  const { data: user } = useMe()
  return (
    <div className="flex min-h-dvh flex-col">
      <header className="border-border/60 bg-bg/80 sticky top-0 z-30 border-b backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-3 px-4 sm:px-6">
          <Link to="/" aria-label={t('common.appName')}>
            <Logo />
          </Link>
          <div className="flex items-center gap-1 sm:gap-2">
            <LanguageToggle />
            <ThemeToggle />
            {user ? (
              <ButtonLink to="/app" size="sm" className="ml-1">
                {t('nav.openApp')}
              </ButtonLink>
            ) : (
              <>
                <ButtonLink to="/login" variant="ghost" size="sm" className="hidden sm:inline-flex">
                  {t('nav.login')}
                </ButtonLink>
                <ButtonLink to="/register" size="sm" className="ml-1">
                  {t('nav.register')}
                </ButtonLink>
              </>
            )}
          </div>
        </div>
      </header>
      <main id="main" className="flex-1">
        <Outlet />
      </main>
      <footer className="border-border text-muted border-t py-8 text-center text-sm">
        <p>© {new Date().getFullYear()} Berq Bank · {t('landing.footer')}</p>
      </footer>
    </div>
  )
}
