import { act, screen, waitFor, within } from '@testing-library/react'
import { db, demoUser, notification } from '@/test/fixtures'
import { renderApp } from '@/test/render'

/** jsdom'da EventSource yok; testte sunucunun gönderdiği olayları elle tetikleyebileceğimiz bir sahte sürüm. */
class FakeEventSource {
  static instances: FakeEventSource[] = []
  url: string
  onerror: (() => void) | null = null
  private listeners = new Map<string, ((e: MessageEvent) => void)[]>()
  closed = false

  constructor(url: string) {
    this.url = url
    FakeEventSource.instances.push(this)
  }

  addEventListener(type: string, fn: (e: MessageEvent) => void) {
    this.listeners.set(type, [...(this.listeners.get(type) ?? []), fn])
  }

  emit(type: string, data: unknown) {
    this.listeners.get(type)?.forEach((fn) => fn(new MessageEvent(type, { data: JSON.stringify(data) })))
  }

  close() {
    this.closed = true
  }
}

describe('notifications', () => {
  beforeEach(() => {
    db.user = demoUser
    FakeEventSource.instances = []
    vi.stubGlobal('EventSource', FakeEventSource)
  })
  afterEach(() => vi.unstubAllGlobals())

  it('shows the unread count and renders notifications in the user language', async () => {
    db.notifications = [
      notification(),
      notification({ id: 2, type: 'BUDGET_EXCEEDED', category: 'FOOD', amount: 2000, counterpartyName: undefined }),
      notification({ id: 3, type: 'SCHEDULED_TRANSFER_FAILED', errorCode: 'INSUFFICIENT_FUNDS', amount: 17500, counterpartyName: 'Mehmet Demir', read: true }),
    ]
    const { user } = renderApp('/app/settings')

    const bell = await screen.findByRole('button', { name: 'Bildirimler: 2 okunmamış bildirim' })
    await user.click(bell)
    const panel = await screen.findByRole('region', { name: 'Bildirimler' })
    expect(within(panel).getByText('Can Öztürk size ₺1.500,00 gönderdi')).toBeInTheDocument()
    expect(within(panel).getByText('Yemek bütçenizi aştınız (₺2.000,00)')).toBeInTheDocument()
    expect(within(panel).getByText('Mehmet Demir için ₺17.500,00 talimatı çalıştırılamadı: Yetersiz bakiye.')).toBeInTheDocument()

    await user.click(within(panel).getByRole('button', { name: 'Tümünü okundu say' }))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Bildirimler' })).toBeInTheDocument())
  })

  it('opens a live stream and reacts to pushed events', async () => {
    renderApp('/app/settings')
    await waitFor(() => expect(FakeEventSource.instances).toHaveLength(1))
    expect(FakeEventSource.instances[0]!.url).toBe('/api/notifications/stream')

    db.notifications = [notification({ id: 5, counterpartyName: 'Ayşe Kaya', amount: 42 })]
    act(() => FakeEventSource.instances[0]!.emit('notification', db.notifications[0]))

    // Toast ve zil sayacı sayfa yenilenmeden güncellenir
    expect(await screen.findByText('Ayşe Kaya size ₺42,00 gönderdi')).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Bildirimler: 1 okunmamış bildirim' })).toBeInTheDocument()
  })

  it('closes the stream when leaving the app', async () => {
    const { router } = renderApp('/app/settings')
    await waitFor(() => expect(FakeEventSource.instances).toHaveLength(1))
    await act(() => router.navigate('/'))
    await waitFor(() => expect(FakeEventSource.instances[0]!.closed).toBe(true))
  })
})
