import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { toast } from 'sonner'
import { FullPageSpinner } from '@/components/ui/Spinner'
import { useLogout } from './api'

/**
 * Çıkış ayrı ve korumasız bir rotada yapılır. Uygulama kabuğu içinden yapılsaydı, oturum sıfırlanır sıfırlanmaz
 * RequireAuth kabuğu kaldırıp kendi yönlendirmesini yapar ve "çıkış yapıldı" akışıyla yarışırdı.
 */
export function LogoutPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const logout = useLogout()
  const started = useRef(false)

  useEffect(() => {
    if (started.current) return // StrictMode'da efekt iki kez çalışır
    started.current = true
    void logout
      .mutateAsync()
      .catch(() => undefined)
      .finally(() => {
        toast.success(t('auth.loggedOut'))
        void navigate('/login', { replace: true })
      })
  }, [logout, navigate, t])

  return <FullPageSpinner label={t('common.loading')} />
}
