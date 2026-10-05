import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { AppProviders } from '@/app/providers'
import { createQueryClient } from '@/app/queryClient'
import { routes } from '@/app/router'

/** Uygulamayı gerçek route ağacı ve provider'larıyla, verilen adreste başlatır. */
export function renderApp(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  const queryClient = createQueryClient()
  queryClient.setDefaultOptions({ queries: { retry: false, staleTime: Infinity, refetchOnWindowFocus: false } })
  // delay: null → tuşlar arasında zamanlayıcı beklenmez; uzun IBAN yazımı testleri yavaşlatmaz
  const user = userEvent.setup({ delay: null })
  const utils = render(
    <AppProviders queryClient={queryClient}>
      <RouterProvider router={router} />
    </AppProviders>,
  )
  return { ...utils, user, router, queryClient }
}
