import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { db, demoUser, RECIPIENT_IBAN, transaction } from '@/test/fixtures'
import { renderApp } from '@/test/render'
import { server } from '@/test/server'

type User = ReturnType<typeof renderApp>['user']

async function goToReview(user: User, amount: string) {
  await user.type(await screen.findByLabelText('Alıcı IBAN'), RECIPIENT_IBAN.slice(2))
  expect(await screen.findByText(/Alıcı: Ayşe K\*\*\*/)).toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Devam' }))

  // Yalnızca alıcıyla aynı para birimindeki (TRY) hesap seçilebilir
  expect(await screen.findByRole('radio', { name: /Vadesiz TL Hesabı/ })).toBeChecked()
  expect(screen.queryByRole('radio', { name: /Dolar Hesabı/ })).not.toBeInTheDocument()

  await user.type(screen.getByLabelText('Tutar'), amount)
  await user.type(screen.getByLabelText(/Açıklama/), 'Kira')
  await user.click(screen.getByRole('radio', { name: 'Kira' }))
  await user.click(screen.getByRole('button', { name: 'Devam' }))
  return screen.findByRole('heading', { name: 'Bilgileri kontrol edin' })
}

describe('transfer wizard', () => {
  beforeEach(() => {
    db.user = demoUser
  })

  it('sends money through recipient → amount → review → done', async () => {
    const { user } = renderApp('/app/transfer')
    await goToReview(user, '1.250,50')

    expect(screen.getByText('₺3.749,50')).toBeInTheDocument() // kalan bakiye önizlemesi
    await user.click(screen.getByRole('button', { name: '₺1.250,50 gönder' }))

    expect(await screen.findByRole('heading', { name: 'Transfer başarılı' })).toBeInTheDocument()
    expect(screen.getByText('BQ7K2M9X4TQ1HZ8C')).toBeInTheDocument()
    expect(db.transferRequests).toHaveLength(1)
    expect(db.transferRequests[0]!.body).toEqual({
      fromAccountId: 1,
      toIban: RECIPIENT_IBAN,
      amount: 1250.5,
      description: 'Kira',
      category: 'RENT',
    })
    expect(db.transferRequests[0]!.idempotencyKey).toMatch(/^[0-9a-f-]{36}$/)
  })

  it('blocks amounts above the balance before calling the API', async () => {
    const { user } = renderApp('/app/transfer')
    await user.type(await screen.findByLabelText('Alıcı IBAN'), RECIPIENT_IBAN.slice(2))
    await screen.findByText(/Alıcı: Ayşe/)
    await user.click(screen.getByRole('button', { name: 'Devam' }))
    await user.type(await screen.findByLabelText('Tutar'), '999999')
    await user.click(screen.getByRole('button', { name: 'Devam' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Tutar bakiyenizi aşıyor.')
    expect(db.transferRequests).toHaveLength(0)
  })

  it('retries with the SAME idempotency key after a network failure', async () => {
    let attempts = 0
    server.use(
      http.post('/api/transactions/transfer', async ({ request }) => {
        attempts += 1
        db.transferRequests.push({ body: await request.json(), idempotencyKey: request.headers.get('Idempotency-Key') })
        if (attempts === 1) return HttpResponse.error()
        return HttpResponse.json(transaction({ amount: 10 }), { status: 200, headers: { 'Idempotent-Replayed': 'true' } })
      }),
    )
    const { user } = renderApp('/app/transfer')
    await goToReview(user, '10')

    await user.click(screen.getByRole('button', { name: /gönder$/ }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Sunucuya ulaşılamıyor')
    await user.click(screen.getByRole('button', { name: /gönder$/ }))
    expect(await screen.findByRole('heading', { name: 'Transfer başarılı' })).toBeInTheDocument()

    const [first, second] = db.transferRequests
    expect(second!.idempotencyKey).toBe(first!.idempotencyKey)
  })

  it('surfaces business errors from the server in the user language', async () => {
    server.use(
      http.post('/api/transactions/transfer', () =>
        HttpResponse.json({ status: 422, code: 'DAILY_LIMIT_EXCEEDED', remaining: 1500 }, { status: 422 }),
      ),
    )
    const { user } = renderApp('/app/transfer')
    await goToReview(user, '100')
    await user.click(screen.getByRole('button', { name: /gönder$/ }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Bugün kalan limit: ₺1.500,00')
  })

  it('pre-fills the recipient from a link and skips to the amount step', async () => {
    renderApp(`/app/transfer?to=${RECIPIENT_IBAN}`)
    expect(await screen.findByRole('heading', { name: 'Ne kadar göndermek istiyorsunuz?' })).toBeInTheDocument()
    expect(screen.getByText('TR33 •••• 1326')).toBeInTheDocument()
  })

  it('reports unknown IBANs inline and keeps Continue disabled', async () => {
    const { user } = renderApp('/app/transfer')
    await user.type(await screen.findByLabelText('Alıcı IBAN'), '000000000000000000000001')
    await waitFor(() => expect(screen.getByText(/hesap bulunamadı/)).toBeInTheDocument())
    expect(screen.getByRole('button', { name: 'Devam' })).toBeDisabled()
  })
})
