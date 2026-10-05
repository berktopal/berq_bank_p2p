import i18n from '@/i18n'
import { formatMoney, initials, parseAmount } from './format'

describe('parseAmount', () => {
  it.each([
    ['1250', 1250],
    ['1.250,50', 1250.5],
    ['1,250.50', 1250.5],
    ['12,5', 12.5],
    ['0.01', 0.01],
    ['1.250', 1250],
    ['1250.', 1250],
    ['₺ 99,90', 99.9],
  ])('parses %s → %d', (input, expected) => {
    expect(parseAmount(input)).toBe(expected)
  })

  it.each(['', 'abc', '-5', '12a'])('rejects %s', (input) => {
    expect(parseAmount(input)).toBeNull()
  })
})

describe('formatMoney', () => {
  it('uses the active locale and an explicit sign when asked', async () => {
    await i18n.changeLanguage('tr')
    expect(formatMoney(1250.5, 'TRY')).toBe('₺1.250,50')
    expect(formatMoney(-20, 'TRY', { signed: true })).toBe('−₺20,00')
    await i18n.changeLanguage('en')
    expect(formatMoney(1250.5, 'USD', { signed: true })).toBe('+$1,250.50')
    // en-US varsayılanı "TRY 9,000.00" olurdu
    expect(formatMoney(9000, 'TRY')).toBe('₺9,000.00')
  })
})

it('builds initials with Turkish casing', () => {
  expect(initials('ilker şahin')).toBe('İŞ')
})

it('pluralises counts in English but not in Turkish', async () => {
  await i18n.changeLanguage('en')
  expect(i18n.t('dashboard.acrossAccounts', { count: 1 })).toBe('1 account')
  expect(i18n.t('dashboard.acrossAccounts', { count: 3 })).toBe('3 accounts')
  expect(i18n.t('transactions.results', { count: 1 })).toBe('1 transaction')
  await i18n.changeLanguage('tr')
  expect(i18n.t('dashboard.acrossAccounts', { count: 1 })).toBe('1 hesap')
  expect(i18n.t('dashboard.acrossAccounts', { count: 3 })).toBe('3 hesap')
})
