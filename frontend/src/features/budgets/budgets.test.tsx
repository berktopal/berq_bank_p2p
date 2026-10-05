import { screen, waitFor, within } from '@testing-library/react'
import { db, demoUser } from '@/test/fixtures'
import { renderApp } from '@/test/render'

describe('budgets', () => {
  beforeEach(() => {
    db.user = demoUser
  })

  it('shows progress with status text, not just colour', async () => {
    db.budgets = [
      { id: 1, category: 'FOOD', currency: 'TRY', monthlyLimit: 2000, spent: 1700, remaining: 300, percent: 85, status: 'WARNING' },
      { id: 2, category: 'SHOPPING', currency: 'TRY', monthlyLimit: 1000, spent: 1200, remaining: 0, percent: 120, status: 'EXCEEDED' },
    ]
    renderApp('/app/budgets')

    const food = await screen.findByRole('meter', { name: 'Yemek' })
    expect(food).toHaveAttribute('aria-valuenow', '85')
    expect(screen.getByText('%85 · Yaklaşıyor')).toBeInTheDocument()
    expect(screen.getByText('Kalan: ₺300,00')).toBeInTheDocument()

    // Aşılan bütçede çubuk %100'de durur, aşım miktarı yazılır
    expect(screen.getByRole('meter', { name: 'Alışveriş' })).toHaveAttribute('aria-valuenow', '100')
    expect(screen.getByText('₺200,00 aşıldı')).toBeInTheDocument()
  })

  it('creates a budget with a Turkish-formatted limit', async () => {
    const { user } = renderApp('/app/budgets')
    await user.click((await screen.findAllByRole('button', { name: 'Bütçe ekle' }))[0]!)
    const dialog = await screen.findByRole('dialog')
    await user.selectOptions(within(dialog).getByLabelText('Kategori'), 'ENTERTAINMENT')
    await user.type(within(dialog).getByLabelText('Aylık limit'), '1.500')
    await user.click(within(dialog).getByRole('button', { name: 'Kaydet' }))

    await waitFor(() =>
      expect(db.calls).toContainEqual({
        method: 'PUT',
        path: '/budgets',
        body: { category: 'ENTERTAINMENT', currency: 'TRY', monthlyLimit: 1500 },
      }),
    )
  })
})
