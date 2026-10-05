import { expect, test, type Page } from '@playwright/test'

async function loginAsDemo(page: Page) {
  await page.goto('/login')
  await page.getByRole('button', { name: 'Demo ile giriş yap' }).click()
  await expect(page).toHaveURL(/\/app$/)
}

function uniqueTckn(): string {
  // Algoritmaya uygun rastgele T.C. kimlik no (backend ile aynı kural)
  const d = [1 + Math.floor(Math.random() * 9), ...Array.from({ length: 8 }, () => Math.floor(Math.random() * 10))]
  const odd = d[0]! + d[2]! + d[4]! + d[6]! + d[8]!
  const even = d[1]! + d[3]! + d[5]! + d[7]!
  d.push((((odd * 7 - even) % 10) + 10) % 10)
  d.push(d.reduce((a, b) => a + b, 0) % 10)
  return d.join('')
}

test('demo user sends money to a saved recipient and finds it in history', async ({ page }) => {
  await loginAsDemo(page)
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Deniz')

  await page.goto('/app/transfer')
  await page.getByRole('button', { name: /Ev sahibi/ }).click()
  await expect(page.getByText(/Alıcı: Mehmet D/)).toBeVisible()
  await page.getByRole('button', { name: 'Devam' }).click()

  const description = `E2E ${Date.now()}`
  await page.getByLabel('Tutar').fill('12,34')
  await page.getByLabel(/Açıklama/).fill(description)
  await page.getByText('Fatura', { exact: true }).click()
  await page.getByRole('button', { name: 'Devam' }).click()

  await expect(page.getByRole('heading', { name: 'Bilgileri kontrol edin' })).toBeVisible()
  await page.getByRole('button', { name: '₺12,34 gönder' }).click()
  await expect(page.getByRole('heading', { name: 'Transfer başarılı' })).toBeVisible()
  const reference = (await page.getByText(/^BQ[0-9A-Z]{14}$/).textContent())!

  await page.getByRole('link', { name: 'Dekontu görüntüle' }).click()
  await expect(page.getByRole('heading', { name: 'İşlem dekontu' })).toBeVisible()
  await expect(page.getByText(reference).first()).toBeVisible()

  await page.goto('/app/transactions')
  await page.getByRole('searchbox', { name: 'Ara' }).fill(description)
  await expect(page.getByText('1 işlem')).toBeVisible()
  await expect(page.getByText('−₺12,34')).toBeVisible()
})

test('a new customer registers, gets a current account and opens a USD account', async ({ page }) => {
  const email = `e2e-${Date.now()}@test.local`
  await page.goto('/register')
  await page.getByLabel('Ad', { exact: true }).fill('Ece')
  await page.getByLabel('Soyad').fill('Test')
  await page.getByLabel('T.C. kimlik no').fill(uniqueTckn())
  await page.getByLabel('E-posta').fill(email)
  await page.getByLabel('Şifre', { exact: true }).fill('Secret123')
  await page.getByRole('button', { name: 'Hesabımı aç' }).click()
  await expect(page).toHaveURL(/\/app$/)

  await page.goto('/app/accounts')
  // Demo profilinde yeni müşteriye 2.500 TL deneme bakiyesi tanımlanır
  await expect(page.getByRole('article', { name: 'Vadesiz TL Hesabı' })).toContainText('₺2.500,00')

  await page.getByRole('button', { name: 'Yeni hesap aç' }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('Hesap adı').fill('Seyahat')
  await dialog.getByRole('radio', { name: 'USD' }).click()
  await dialog.getByRole('button', { name: 'Yeni hesap aç' }).click()
  await expect(page.getByRole('article', { name: 'Seyahat' })).toContainText('$0,00')
})

test('protected pages redirect to login and the session ends on logout', async ({ page }, testInfo) => {
  await page.goto('/app/analytics')
  await expect(page).toHaveURL(/\/login\?next=/)

  await page.getByRole('button', { name: 'Demo ile giriş yap' }).click()
  await expect(page).toHaveURL(/\/app\/analytics$/)
  await expect(page.getByRole('heading', { name: 'Harcama analizi' })).toBeVisible()

  await page.goto('/app/settings')
  await expect(page.getByRole('heading', { name: 'Ayarlar' })).toBeVisible()
  // Mobilde çıkış "Daha fazla" menüsünde, masaüstünde kenar çubuğunda
  if (testInfo.project.name === 'mobile') {
    await page.getByRole('button', { name: 'Daha fazla' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Çıkış yap' }).click()
  } else {
    await page.getByRole('complementary').getByRole('button', { name: 'Çıkış yap' }).click()
  }
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('Güvenli şekilde çıkış yaptınız.')).toBeVisible()

  await page.goto('/app')
  await expect(page).toHaveURL(/\/login/)
})
