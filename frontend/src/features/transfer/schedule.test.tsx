import { screen, waitFor } from '@testing-library/react'
import { db, demoUser, RECIPIENT_IBAN } from '@/test/fixtures'
import { renderApp } from '@/test/render'
import { dayKey } from '@/lib/format'

function inDays(n: number) {
  return dayKey(new Date(Date.now() + n * 86_400_000).toISOString())
}

describe('scheduling from the transfer wizard', () => {
  beforeEach(() => {
    db.user = demoUser
  })

  it('creates a monthly schedule instead of sending immediately', async () => {
    const { user } = renderApp(`/app/transfer?to=${RECIPIENT_IBAN}`)
    await user.type(await screen.findByLabelText('Tutar'), '17.500')
    await user.click(screen.getByRole('radio', { name: 'Düzenli' }))
    await user.click(screen.getByRole('radio', { name: 'Her ay' }))
    const start = inDays(5)
    await user.clear(screen.getByLabelText('Başlangıç'))
    await user.type(screen.getByLabelText('Başlangıç'), start)
    await user.click(screen.getByRole('button', { name: 'Devam' }))

    expect(await screen.findByText('Her ay')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Talimatı oluştur' }))

    expect(await screen.findByRole('heading', { name: 'Talimat oluşturuldu' })).toBeInTheDocument()
    expect(db.transferRequests).toHaveLength(0)
    expect(db.calls).toContainEqual({
      method: 'POST',
      path: '/scheduled-transfers',
      body: expect.objectContaining({ fromAccountId: 1, toIban: RECIPIENT_IBAN, amount: 17500, frequency: 'MONTHLY', startDate: start }),
    })
  })

  it('lets future-dated transfers exceed today’s balance but not be dated in the past', async () => {
    const { user } = renderApp(`/app/transfer?to=${RECIPIENT_IBAN}`)
    await user.type(await screen.findByLabelText('Tutar'), '999999')
    await user.click(screen.getByRole('radio', { name: 'İleri tarih' }))
    await user.clear(screen.getByLabelText('Tarih'))
    await user.type(screen.getByLabelText('Tarih'), inDays(-1))
    await user.click(screen.getByRole('button', { name: 'Devam' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Talimat tarihleri geçersiz')

    await user.clear(screen.getByLabelText('Tarih'))
    await user.type(screen.getByLabelText('Tarih'), inDays(3))
    await user.click(screen.getByRole('button', { name: 'Devam' }))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Talimatı oluştur' })).toBeInTheDocument())
  })
})
