import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { db, demoUser, page, transaction } from '@/test/fixtures'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

describe('transactions page', () => {
  beforeEach(() => {
    db.user = demoUser
  })

  it('keeps filters in the URL and sends them to the API', async () => {
    const seen: URLSearchParams[] = []
    server.use(
      http.get('/api/transactions', ({ request }) => {
        seen.push(new URL(request.url).searchParams)
        return HttpResponse.json(page([transaction(), transaction({ id: 101, direction: 'INCOMING', amount: 75 })]))
      }),
    )
    const { user, router } = renderApp('/app/transactions')

    expect(await screen.findByText('2 işlem')).toBeInTheDocument()
    expect(screen.getByText('+₺75,00')).toBeInTheDocument()
    expect(screen.getByText('−₺250,00')).toBeInTheDocument()

    await user.click(screen.getByRole('radio', { name: 'Gelen' }))
    await waitFor(() => expect(router.state.location.search).toContain('direction=INCOMING'))
    await waitFor(() => expect(seen.at(-1)?.get('direction')).toBe('INCOMING'))

    await user.type(screen.getByRole('searchbox', { name: 'Ara' }), 'kira')
    await waitFor(() => expect(seen.at(-1)?.get('q')).toBe('kira'))
    expect(router.state.location.search).toContain('q=kira')

    // CSV linki aynı filtreleri taşır (sayfalama hariç)
    const csv = screen.getByRole('link', { name: /CSV indir/ })
    expect(csv.getAttribute('href')).toBe('/api/transactions/export?direction=INCOMING&q=kira')
  })

  it('shows an empty state with a hint when filters match nothing', async () => {
    renderApp('/app/transactions?direction=OUTGOING')
    expect(await screen.findByText('Bu kriterlere uyan işlem yok.')).toBeInTheDocument()
    expect(screen.getByText('Filtreleri değiştirmeyi ya da temizlemeyi deneyin.')).toBeInTheDocument()
  })

  it('logs the user out of the UI when the session expires mid-use', async () => {
    const { router } = renderApp('/app/transactions')
    await screen.findByText('Bu kriterlere uyan işlem yok.')
    db.user = null
    server.use(http.get('/api/contacts', () => HttpResponse.json({ code: 'UNAUTHENTICATED' }, { status: 401 })))
    await router.navigate('/app/contacts')
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(await screen.findByText('Oturumunuzun süresi doldu. Lütfen tekrar giriş yapın.')).toBeInTheDocument()
  })
})
