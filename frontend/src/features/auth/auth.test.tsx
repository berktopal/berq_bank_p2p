import { screen, waitFor } from '@testing-library/react'
import { db, demoUser } from '@/test/fixtures'
import { renderApp } from '@/test/render'

describe('authentication flow', () => {
  it('redirects anonymous users to login and back to where they were after signing in', async () => {
    const { user, router } = renderApp('/app/transactions?direction=INCOMING')

    expect(await screen.findByRole('heading', { name: 'Tekrar hoş geldiniz' })).toBeInTheDocument()
    expect(router.state.location.search).toContain('next=')

    await user.type(screen.getByLabelText('E-posta'), demoUser.email)
    await user.type(screen.getByLabelText('Şifre'), 'Demo1234')
    await user.click(screen.getByRole('button', { name: 'Giriş yap' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/app/transactions'))
    expect(router.state.location.search).toBe('?direction=INCOMING')
  })

  it('validates on the client before calling the API', async () => {
    const { user } = renderApp('/login')
    await user.click(await screen.findByRole('button', { name: 'Giriş yap' }))
    expect(await screen.findAllByText('Bu alan zorunludur.')).toHaveLength(2)
    expect(screen.getByLabelText('E-posta')).toHaveAttribute('aria-invalid', 'true')
  })

  it('shows the translated server error for wrong credentials', async () => {
    const { user } = renderApp('/login')
    await user.type(await screen.findByLabelText('E-posta'), demoUser.email)
    await user.type(screen.getByLabelText('Şifre'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Giriş yap' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('E-posta veya şifre hatalı.')
  })

  it('ignores open-redirect attempts in ?next=', async () => {
    const { user, router } = renderApp('/login?next=https%3A%2F%2Fevil.example')
    await user.type(await screen.findByLabelText('E-posta'), demoUser.email)
    await user.type(screen.getByLabelText('Şifre'), 'Demo1234')
    await user.click(screen.getByRole('button', { name: 'Giriş yap' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/app'))
  })

  it('keeps signed-in users away from the login page', async () => {
    db.user = demoUser
    const { router } = renderApp('/login')
    await waitFor(() => expect(router.state.location.pathname).toBe('/app'))
  })
})
