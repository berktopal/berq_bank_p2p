// İlk boyamadan önce temayı uygular (beyaz flaş olmasın). CSP 'self' ile uyumlu olması için harici dosya.
;(function () {
  try {
    var stored = localStorage.getItem('berq.theme')
    var dark = stored ? stored === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches
    document.documentElement.dataset.theme = dark ? 'dark' : 'light'
  } catch (e) {
    document.documentElement.dataset.theme = 'light'
  }
})()
