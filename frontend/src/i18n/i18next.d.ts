import 'i18next'
import type { Resources } from './tr'

// Çeviri anahtarları derleme zamanında denetlenir: t('transfer.olmayanAnahtar') hata verir.
declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: 'translation'
    resources: { translation: Resources }
  }
}
