import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterAll, afterEach, beforeAll, beforeEach } from 'vitest'
import i18n from '@/i18n'
import { resetDb } from './fixtures'
import { server } from './server'

// jsdom'da olmayan tarayıcı API'leri
window.matchMedia ??= ((query: string) => ({
  matches: false,
  media: query,
  onchange: null,
  addEventListener: () => {},
  removeEventListener: () => {},
  addListener: () => {},
  removeListener: () => {},
  dispatchEvent: () => false,
})) as typeof window.matchMedia
globalThis.ResizeObserver ??= class {
  observe() {}
  unobserve() {}
  disconnect() {}
} as unknown as typeof ResizeObserver
window.scrollTo = () => {}
// jsdom native <dialog> açma/kapamayı uygulamıyor; tarayıcı davranışını taklit et
HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
  this.setAttribute('open', '')
}
HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
  this.removeAttribute('open')
  this.dispatchEvent(new Event('close'))
}

// Paralel koşan dosyalar jsdom'u yavaşlatır; lazy yüklenen sayfalar (grafikler) ilk seferde birkaç sn sürebilir
configure({ asyncUtilTimeout: 8000 })

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
beforeEach(async () => {
  resetDb()
  await i18n.changeLanguage('tr')
  document.cookie = 'XSRF-TOKEN=test-csrf-token; path=/'
})
afterEach(() => {
  cleanup()
  server.resetHandlers()
})
afterAll(() => server.close())
