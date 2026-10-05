import { Compass, TriangleAlert } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { isRouteErrorResponse, useRouteError } from 'react-router'
import { Button, ButtonLink } from '@/components/ui/Button'

export function NotFoundPage() {
  const { t } = useTranslation()
  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-4 px-4 py-24 text-center">
      <span className="bg-primary-soft text-primary grid size-16 place-items-center rounded-2xl">
        <Compass className="size-8" aria-hidden />
      </span>
      <p className="text-primary text-sm font-bold">404</p>
      <h1 className="text-2xl font-bold">{t('errors.notFoundTitle')}</h1>
      <p className="text-muted">{t('errors.notFoundBody')}</p>
      <ButtonLink to="/">{t('errors.goHome')}</ButtonLink>
    </div>
  )
}

/** Route düzeyinde hata sınırı: bir sayfadaki render hatası tüm uygulamayı beyaz ekrana düşürmez. */
export function RouteErrorBoundary() {
  const { t } = useTranslation()
  const error = useRouteError()
  if (isRouteErrorResponse(error) && error.status === 404) return <NotFoundPage />
  if (import.meta.env.DEV) console.error(error)
  return (
    <div role="alert" className="mx-auto flex max-w-md flex-col items-center gap-4 px-4 py-24 text-center">
      <span className="bg-danger-soft text-danger grid size-16 place-items-center rounded-2xl">
        <TriangleAlert className="size-8" aria-hidden />
      </span>
      <h1 className="text-2xl font-bold">{t('errors.boundaryTitle')}</h1>
      <p className="text-muted">{t('errors.boundaryBody')}</p>
      <Button onClick={() => window.location.reload()}>{t('errors.reload')}</Button>
    </div>
  )
}
