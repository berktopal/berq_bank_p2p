import { defineConfig, devices } from '@playwright/test'

/**
 * Uçtan uca testler, derlenmiş SPA'yı sunan gerçek Spring Boot uygulamasına karşı koşar:
 *   cd p2p-transfer && ./mvnw spring-boot:test-run -Dspring-boot.run.main-class=p2p_transfer.DevServer
 * (gömülü PostgreSQL + demo verisi). CI bu sunucuyu ayrı bir adımda başlatır.
 */
export default defineConfig({
  testDir: './e2e',
  // Sunucu soğuk başlar (JIT, BCrypt); çok kullanıcılı senaryolar birkaç giriş içerir
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8080',
    locale: 'tr-TR',
    timezoneId: 'Europe/Istanbul',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile', use: { ...devices['Pixel 7'] } },
  ],
})
