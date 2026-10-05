import { expect, test, type Page, type TestInfo } from '@playwright/test'

async function login(page: Page, email: string) {
  await page.goto('/login')
  await page.getByLabel('E-posta').fill(email)
  await page.getByLabel('Şifre', { exact: true }).fill('Demo1234')
  await page.getByRole('button', { name: 'Giriş yap', exact: true }).click()
  await expect(page).toHaveURL(/\/app$/)
}

async function logout(page: Page, testInfo: TestInfo) {
  if (testInfo.project.name === 'mobile') {
    await page.getByRole('button', { name: 'Daha fazla' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Çıkış yap' }).click()
  } else {
    await page.getByRole('complementary').getByRole('button', { name: 'Çıkış yap' }).click()
  }
  await expect(page).toHaveURL(/\/login$/)
}

test('money request round trip between two users with a notification for the requester', async ({ page }, testInfo) => {
  const note = `E2E istek ${Date.now()}`

  // 1) Deniz, kayıtlı alıcısı Zeynep'ten 75 TL istiyor
  await login(page, 'demo@berqbank.dev')
  await page.goto('/app/requests')
  await page.getByRole('button', { name: 'Para iste' }).first().click()
  const dialog = page.getByRole('dialog')
  await dialog.getByRole('button', { name: 'Zeynep' }).click()
  await expect(dialog.getByText(/Zeynep Ç/)).toBeVisible()
  await dialog.getByLabel('Tutar').fill('75')
  await dialog.getByLabel(/Açıklama/).fill(note)
  await dialog.getByRole('button', { name: 'Para iste' }).click()
  await expect(page.getByText(note)).toBeVisible()
  await logout(page, testInfo)

  // 2) Zeynep isteği görür ve öder
  await login(page, 'zeynep@berqbank.dev')
  await page.goto('/app/requests')
  const row = page.getByRole('listitem').filter({ hasText: note })
  await row.getByRole('button', { name: 'Öde' }).click()
  await page.getByRole('dialog').getByRole('button', { name: /Öde · ₺75,00/ }).click()
  await expect(row.getByText('Ödendi')).toBeVisible()
  await logout(page, testInfo)

  // 3) Deniz'in isteği ödendi olarak görünür ve bildirimi vardır
  await login(page, 'demo@berqbank.dev')
  await page.getByRole('button', { name: /^Bildirimler/ }).click()
  await expect(page.getByRole('region', { name: 'Bildirimler' }).getByText('Zeynep Çelik ₺75,00 tutarındaki isteğinizi ödedi').first()).toBeVisible()
  await page.goto('/app/requests?tab=OUT')
  await expect(page.getByRole('listitem').filter({ hasText: note }).getByText('Ödendi')).toBeVisible()
})

test('budget and scheduled transfer can be created from the UI', async ({ page }) => {
  await login(page, 'demo@berqbank.dev')

  await page.goto('/app/budgets')
  await page.getByRole('button', { name: 'Bütçe ekle' }).first().click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('Kategori').selectOption('HEALTH')
  await dialog.getByLabel('Aylık limit').fill('3.000')
  await dialog.getByRole('button', { name: 'Kaydet' }).click()
  await expect(page.getByRole('meter', { name: 'Sağlık' })).toBeVisible()

  const note = `E2E talimat ${Date.now()}`
  await page.goto('/app/transfer?when=RECURRING')
  await page.getByRole('button', { name: /Ayşe/ }).first().click()
  await page.getByRole('button', { name: 'Devam' }).click()
  await page.getByLabel('Tutar').fill('50')
  await page.getByLabel(/Açıklama/).fill(note)
  await page.getByRole('radio', { name: 'Her hafta' }).click()
  await page.getByRole('button', { name: 'Devam' }).click()
  await page.getByRole('button', { name: 'Talimatı oluştur' }).click()
  await expect(page.getByRole('heading', { name: 'Talimat oluşturuldu' })).toBeVisible()

  await page.getByRole('link', { name: 'Talimatlarım' }).click()
  await expect(page.getByText(note)).toBeVisible()
})
