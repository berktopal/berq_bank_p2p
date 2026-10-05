import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import en from './en'
import tr from './tr'

export const LANGUAGE_KEY = 'berq.lang'
export type Language = 'tr' | 'en'

function initialLanguage(): Language {
  try {
    const stored = localStorage.getItem(LANGUAGE_KEY)
    if (stored === 'tr' || stored === 'en') return stored
  } catch {
    // localStorage kapalı olabilir (gizli mod)
  }
  return navigator.language?.toLowerCase().startsWith('en') ? 'en' : 'tr'
}

void i18n.use(initReactI18next).init({
  resources: { tr: { translation: tr }, en: { translation: en } },
  lng: initialLanguage(),
  fallbackLng: 'tr',
  interpolation: { escapeValue: false },
  returnNull: false,
})

i18n.on('languageChanged', (lng) => {
  document.documentElement.lang = lng
})
document.documentElement.lang = i18n.language

export function setLanguage(lng: Language) {
  try {
    localStorage.setItem(LANGUAGE_KEY, lng)
  } catch {
    // tercih kalıcı olmaz ama dil yine değişir
  }
  void i18n.changeLanguage(lng)
}

export default i18n
