let user = null, acc = null, myChart = null, targetId = null, currentLang = 'tr';

const translations = {
    tr: {
        loginMsg: "Dijital bankamıza hoş geldiniz", loginBtn: "Giriş Yap", hello: "Merhaba", themeDark: "Karanlık", themeLight: "Aydınlık", logout: "Çıkış Yap",
        quickTransfer: "💸 Hızlı Para Transferi", sendBtn: "Gönder", analysis: "📊 Harcama Dağılımı", recent: "📜 Son Hesap Hareketleri",
        thDate: "Tarih", thName: "İsim", thIban: "IBAN", thAmount: "Tutar", 
        success: "Başarılı", error: "Hata", sent: "Transfer başarıyla gerçekleşti!", warning: "Uyarı", invalidIban: "Lütfen geçerli bir IBAN girin.", toggle: "EN",
        searchPlaceholder: "Ara...", copied: "IBAN Kopyalandı!"
    },
    en: {
        loginMsg: "Welcome to our digital bank", loginBtn: "Login", hello: "Hello", themeDark: "Dark", themeLight: "Light", logout: "Logout",
        quickTransfer: "💸 Quick Money Transfer", sendBtn: "Send", analysis: "📊 Expense Breakdown", recent: "📜 Recent Account Activity",
        thDate: "Date", thName: "Name", thIban: "IBAN", thAmount: "Amount",
        success: "Success", error: "Error", sent: "Transfer completed successfully!", warning: "Warning", invalidIban: "Please enter a valid IBAN.", toggle: "TR",
        searchPlaceholder: "Search...", copied: "IBAN Copied!"
    }
};

function toggleLanguage() {
    currentLang = currentLang === 'tr' ? 'en' : 'tr';
    const l = translations[currentLang];
    
    document.getElementById('lang-login-msg').innerText = l.loginMsg;
    document.getElementById('lang-login-btn').innerText = l.loginBtn;
    document.getElementById('lang-hello').innerText = l.hello;
    document.getElementById('lang-logout').innerText = l.logout;
    document.getElementById('lang-quick-transfer').innerText = l.quickTransfer;
    document.getElementById('lang-send-btn').innerText = l.sendBtn;
    document.getElementById('lang-analysis').innerText = l.analysis;
    document.getElementById('lang-recent').innerText = l.recent;
    document.getElementById('lang-th-date').innerText = l.thDate;
    document.getElementById('lang-th-name').innerText = l.thName;
    document.getElementById('lang-th-iban').innerText = l.thIban;
    document.getElementById('lang-th-amount').innerText = l.thAmount;
    
    document.querySelectorAll('.lang-toggle-text').forEach(el => el.innerText = l.toggle);
    
    const isDark = document.body.getAttribute('data-theme') === 'dark';
    document.querySelectorAll('.lang-theme').forEach(el => el.innerText = isDark ? l.themeLight : l.themeDark);
    
    document.getElementById('searchInput').placeholder = l.searchPlaceholder;

    if(user) refreshData();
}

function togglePassword() { 
    const pw = document.getElementById('password'); 
    const icon = document.getElementById('eyeIcon');
    
    if (pw.type === 'password') {
        pw.type = 'text';
        icon.innerHTML = `<svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>`;
    } else {
        pw.type = 'password';
        icon.innerHTML = `<svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>`;
    }
}

function toggleTheme() {
    const b = document.body;
    const t = b.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
    b.setAttribute('data-theme', t);
    const isDark = t === 'dark';
    const l = translations[currentLang];
    document.querySelectorAll('.lang-theme').forEach(el => el.innerText = isDark ? l.themeLight : l.themeDark);
    if(user) refreshData();
}

function onlyNumbers(input) { input.value = input.value.replace(/[^0-9.]/g, ''); }

function formatIban(input) {
    let v = input.value.replace(/\D/g, '').substring(0, 24);
    input.value = v.replace(/(.{4})/g, '$1 ').trim();
    if(v.length >= 10) checkIban("TR" + v);
    else { document.getElementById('receiverName').innerText = ""; targetId = null; }
}

async function login() {
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const errorMsg = document.getElementById('loginErrorMsg');
    
    // Her yeni denemede hatayı ekrandan kaldır
    errorMsg.style.display = 'none';

    try {
        const res = await fetch('/api/users/login', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({email, password})});
        
        if(res.ok) {
            user = await res.json();
            document.getElementById('loginArea').style.display = 'none';
            document.getElementById('mainDashboard').style.display = 'flex';
            document.getElementById('uName').innerText = user.firstName;
            refreshData();
        } else { 
            // ŞİFRE YANLIŞSA: Pop-up yerine formun içindeki kırmızı yazıyı görünür yap
            errorMsg.innerText = "❌ " + translations[currentLang].error + ": Giriş başarısız!";
            errorMsg.style.display = 'block'; 
        }
    } catch(err) {
        errorMsg.innerText = "❌ Sunucuya bağlanılamadı!";
        errorMsg.style.display = 'block'; 
    }
}

async function refreshData() {
    const accRes = await fetch(`/api/accounts/user/${user.id}`);
    const accounts = await accRes.json();
    acc = accounts[0];
    document.getElementById('uBalance').innerHTML = `${acc.balance.toLocaleString('tr-TR', {minimumFractionDigits: 2})} <span class="acc-currency">TL</span>`;
    
    document.getElementById('uIban').innerText = acc.iban.replace(/(.{4})/g, '$1 ').trim();

    const tRes = await fetch('/api/transactions');
    const allTransactions = await tRes.json();
    const myTransactions = allTransactions.filter(t => t.senderAccount.id === acc.id || t.receiverAccount.id === acc.id);

    const tbody = document.querySelector('#tTable tbody');
    const contactDiv = document.getElementById('quickContacts');
    tbody.innerHTML = ''; contactDiv.innerHTML = '';
    const contacts = {}; const chartData = {};

    myTransactions.reverse().forEach(t => {
        const isSent = t.senderAccount.id === acc.id;
        const amount = t.amount.toLocaleString('tr-TR', {minimumFractionDigits: 2});
        const date = new Date(t.transactionDate).toLocaleDateString(currentLang === 'tr' ? 'tr-TR' : 'en-US');
        
        if (isSent) {
            const rName = t.receiverAccount.user.firstName + " " + t.receiverAccount.user.lastName;
            const rawReceiverIban = t.receiverAccount.iban;
            const rIban = rawReceiverIban.replace(/(.{4})/g, '$1 ').trim(); 
            
            tbody.innerHTML += `<tr><td>${date}</td><td>${rName}</td><td>${rIban}</td><td class="amt-neg">-${amount} TL</td></tr>`;
            chartData[rName] = (chartData[rName] || 0) + t.amount;
            
            if(!contacts[rawReceiverIban]) {
                contacts[rawReceiverIban] = rName;
                contactDiv.innerHTML += `<div class="contact-item" onclick="fillIban('${rawReceiverIban}')">${t.receiverAccount.user.firstName}</div>`;
            }
        } else {
            const sName = t.senderAccount.user.firstName + " " + t.senderAccount.user.lastName;
            const sIban = t.senderAccount.iban.replace(/(.{4})/g, '$1 ').trim();
            tbody.innerHTML += `<tr><td>${date}</td><td>${sName}</td><td>${sIban}</td><td class="amt-pos">+${amount} TL</td></tr>`;
        }
    });
    updateChart(chartData);
}

async function checkIban(fullIban) {
    const info = document.getElementById('receiverName');
    const res = await fetch(`/api/accounts/iban/${fullIban}`);
    if(res.ok) { 
        const t = await res.json(); 
        info.innerText = `✅ ${t.user.firstName} ${t.user.lastName}`; 
        targetId = t.id; 
    } else { info.innerText = "❌"; targetId = null; }
}

function fillIban(iban) { 
    let formattedIban = iban.replace("TR", "").replace(/(.{4})/g, '$1 ').trim();
    document.getElementById('targetIban').value = formattedIban; 
    checkIban(iban); 
}

async function transfer() {
    const l = translations[currentLang];
    if(!targetId) return Swal.fire(l.warning, l.invalidIban, 'warning');
    const amt = document.getElementById('amt').value;
    const res = await fetch('/api/transactions/transfer', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({senderAccountId: acc.id, receiverAccountId: targetId, amount: amt})});
    if(res.ok) { 
        document.getElementById('successSound').play();
        Swal.fire(l.success, l.sent, 'success'); 
        refreshData(); 
    } else { const msg = await res.text(); Swal.fire(l.error, msg, 'error'); }
}

function updateChart(data) {
    const ctx = document.getElementById('expenseChart').getContext('2d');
    if(myChart) myChart.destroy();
    const isDark = document.body.getAttribute('data-theme') === 'dark';
    myChart = new Chart(ctx, {
        type: 'doughnut',
        data: { labels: Object.keys(data), datasets: [{ data: Object.values(data), backgroundColor: ['#3498db', '#6c5ce7', '#e74c3c', '#f1c40f', '#27AE60'] }] },
        options: { maintainAspectRatio: false, plugins: { legend: { position: 'right', labels: { color: isDark ? '#eee' : '#333', font: { size: 10 } } } } }
    });
}

function copyIban() {
    const ibanText = document.getElementById('uIban').innerText.replace(/\s+/g, '');
    navigator.clipboard.writeText(ibanText).then(() => {
        const l = translations[currentLang];
        Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: l.copied, showConfirmButton: false, timer: 2000 });
    });
}

function filterTable() {
    const input = document.getElementById('searchInput').value.toLowerCase();
    const rows = document.querySelectorAll('#tTable tbody tr');
    
    rows.forEach(row => {
        const text = row.innerText.toLowerCase();
        row.style.display = text.includes(input) ? '' : 'none';
    });
}