import { screen, waitFor, within } from '@testing-library/react'
import { db, demoUser, paymentRequest } from '@/test/fixtures'
import { renderApp } from '@/test/render'

describe('money requests', () => {
  beforeEach(() => {
    db.user = demoUser
    db.paymentRequests = [
      paymentRequest(),
      paymentRequest({ id: 32, direction: 'OUTGOING', counterpartyName: 'Zeynep Ç****', amount: 480, description: 'Market' }),
    ]
  })

  it('pays an incoming request from an account in the same currency', async () => {
    const { user } = renderApp('/app/requests')

    const row = (await screen.findByText('Ayşe Kaya istiyor')).closest('li')!
    expect(within(row).getByText('₺250,00')).toBeInTheDocument()
    expect(within(row).getByText('Bekliyor')).toBeInTheDocument()
    await user.click(within(row).getByRole('button', { name: 'Öde' }))

    const dialog = await screen.findByRole('dialog')
    // Yalnızca TRY hesap listelenir, USD hesap seçilemez
    expect(within(dialog).getByRole('radio', { name: /Vadesiz TL Hesabı/ })).toBeChecked()
    expect(within(dialog).queryByRole('radio', { name: /Dolar Hesabı/ })).not.toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: /Öde · ₺250,00/ }))

    await waitFor(() => expect(db.calls).toContainEqual({ method: 'POST', path: '/payment-requests/31/pay', body: { fromAccountId: 1 } }))
    expect(await screen.findByText('Ödendi')).toBeInTheDocument()
  })

  it('declines after confirmation', async () => {
    const { user } = renderApp('/app/requests')
    await user.click(await screen.findByRole('button', { name: 'Reddet' }))
    const dialog = await screen.findByRole('dialog', { name: 'Bu isteği reddetmek istiyor musunuz?' })
    await user.click(within(dialog).getByRole('button', { name: 'Reddet' }))
    await waitFor(() => expect(db.calls).toContainEqual({ method: 'POST', path: '/payment-requests/31/decline' }))
  })

  it('shows sent requests on the second tab with a cancel action', async () => {
    const { user, router } = renderApp('/app/requests')
    await user.click(await screen.findByRole('radio', { name: 'Gönderdiğim' }))
    expect(router.state.location.search).toBe('?tab=OUT')
    expect(await screen.findByText('Zeynep Ç**** kişisinden')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'İsteği iptal et' })).toBeInTheDocument()
  })

  it('highlights pending requests on the dashboard and in the menu badge', async () => {
    renderApp('/app')
    expect(await screen.findByText('Ayşe Kaya sizden ₺250,00 istiyor')).toBeInTheDocument()
    const link = screen.getAllByRole('link', { name: /Para istekleri/ })[0]!
    expect(within(link).getByText('1')).toBeInTheDocument()
  })
})
