import { useEffect, useState, type ReactNode } from 'react'
import { QueryClientProvider, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Toaster, toast } from 'sonner'
import { authKeys, resetSession } from '@/features/auth/api'
import { useTheme } from '@/hooks/useTheme'
import { onUnauthorized } from '@/lib/api'
import { createQueryClient } from './queryClient'

/** Oturum sunucuda düştüğünde (15 dk hareketsizlik) önbelleği temizleyip girişe yönlendirir. */
function SessionWatcher() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  useEffect(
    () =>
      onUnauthorized(() => {
        if (qc.getQueryData(authKeys.me)) {
          toast.warning(t('auth.sessionExpired'), { id: 'session-expired' })
        }
        resetSession(qc, null)
      }),
    [qc, t],
  )
  return null
}

function ThemedToaster() {
  const { resolved } = useTheme()
  return <Toaster theme={resolved} position="top-center" richColors closeButton />
}

export function AppProviders({ children, queryClient }: { children: ReactNode; queryClient?: QueryClient }) {
  const [client] = useState(() => queryClient ?? createQueryClient())
  return (
    <QueryClientProvider client={client}>
      <SessionWatcher />
      {children}
      <ThemedToaster />
    </QueryClientProvider>
  )
}
