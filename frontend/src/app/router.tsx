import type { ComponentType } from 'react'
import { createBrowserRouter, type RouteObject } from 'react-router'
import { AppShell } from '@/components/layout/AppShell'
import { PublicLayout } from '@/components/layout/PublicLayout'
import { LoginPage } from '@/features/auth/LoginPage'
import { LogoutPage } from '@/features/auth/LogoutPage'
import { RegisterPage } from '@/features/auth/RegisterPage'
import { RedirectIfAuthenticated, RequireAuth } from '@/features/auth/RequireAuth'
import { LandingPage } from '@/features/landing/LandingPage'
import { NotFoundPage, RouteErrorBoundary } from './errors'

// Uygulama içi sayfalar ayrı chunk'lara bölünür: landing/login ziyaretçisi grafik kütüphanesini indirmez.
const lazy = (load: () => Promise<{ Component: ComponentType }>) => ({ lazy: load })

export const routes: RouteObject[] = [
  {
    errorElement: <RouteErrorBoundary />,
    children: [
      {
        element: <PublicLayout />,
        children: [
          { index: true, element: <LandingPage /> },
          {
            element: <RedirectIfAuthenticated />,
            children: [
              { path: 'login', element: <LoginPage /> },
              { path: 'register', element: <RegisterPage /> },
            ],
          },
        ],
      },
      { path: 'logout', element: <LogoutPage /> },
      {
        path: 'app',
        element: <RequireAuth />,
        children: [
          {
            element: <AppShell />,
            errorElement: <RouteErrorBoundary />,
            children: [
              { index: true, ...lazy(() => import('@/features/dashboard/DashboardPage').then((m) => ({ Component: m.DashboardPage }))) },
              { path: 'accounts', ...lazy(() => import('@/features/accounts/AccountsPage').then((m) => ({ Component: m.AccountsPage }))) },
              { path: 'transfer', ...lazy(() => import('@/features/transfer/TransferPage').then((m) => ({ Component: m.TransferPage }))) },
              {
                path: 'transactions',
                ...lazy(() => import('@/features/transactions/TransactionsPage').then((m) => ({ Component: m.TransactionsPage }))),
              },
              {
                path: 'transactions/:id',
                ...lazy(() =>
                  import('@/features/transactions/TransactionDetailPage').then((m) => ({ Component: m.TransactionDetailPage })),
                ),
              },
              { path: 'requests', ...lazy(() => import('@/features/requests/RequestsPage').then((m) => ({ Component: m.RequestsPage }))) },
              { path: 'scheduled', ...lazy(() => import('@/features/schedules/SchedulesPage').then((m) => ({ Component: m.SchedulesPage }))) },
              { path: 'budgets', ...lazy(() => import('@/features/budgets/BudgetsPage').then((m) => ({ Component: m.BudgetsPage }))) },
              { path: 'contacts', ...lazy(() => import('@/features/contacts/ContactsPage').then((m) => ({ Component: m.ContactsPage }))) },
              { path: 'analytics', ...lazy(() => import('@/features/analytics/AnalyticsPage').then((m) => ({ Component: m.AnalyticsPage }))) },
              { path: 'settings', ...lazy(() => import('@/features/settings/SettingsPage').then((m) => ({ Component: m.SettingsPage }))) },
              { path: '*', element: <NotFoundPage /> },
            ],
          },
        ],
      },
      { element: <PublicLayout />, children: [{ path: '*', element: <NotFoundPage /> }] },
    ],
  },
]

export const createRouter = () => createBrowserRouter(routes)
