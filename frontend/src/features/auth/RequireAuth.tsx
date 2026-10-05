import { useTranslation } from 'react-i18next'
import { Navigate, Outlet, useLocation, useSearchParams } from 'react-router'
import { FullPageSpinner } from '@/components/ui/Spinner'
import { safeNext } from '@/lib/navigation'
import { useMe } from './api'

/** Oturum yoksa girişe yönlendirir; girişten sonra kullanıcı kaldığı sayfaya döner (?next=). */
export function RequireAuth() {
  const { t } = useTranslation()
  const { data: user, isPending } = useMe()
  const location = useLocation()

  if (isPending) return <FullPageSpinner label={t('common.loading')} />
  if (!user) {
    const next = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?next=${next}`} replace />
  }
  return <Outlet />
}

/** Giriş yapmış kullanıcı login/register sayfalarını görmez; ?next= varsa oraya gider (LoginPage ile aynı kural). */
export function RedirectIfAuthenticated() {
  const { t } = useTranslation()
  const { data: user, isPending } = useMe()
  const [params] = useSearchParams()
  if (isPending) return <FullPageSpinner label={t('common.loading')} />
  if (user) return <Navigate to={safeNext(params.get('next'))} replace />
  return <Outlet />
}
