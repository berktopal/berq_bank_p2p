# 🏦 Berq Bank - P2P Para Transfer Sistemi

Berq Bank, kullanıcıların güvenli bir şekilde giriş yapabildiği, hesap bakiyelerini takip edebildiği ve anlık olarak IBAN üzerinden para transferi gerçekleştirebildiği modern bir dijital bankacılık uygulamasıdır.

## ✨ Özellikler

* 🔐 **Güvenli Giriş Sistemi:** Hatalı şifre denemelerinde kullanıcı dostu, sayfa içi hata bildirimleri.
* 💸 **P2P Para Transferi:** IBAN üzerinden anlık para gönderimi ve bakiye güncelleme.
* 📊 **Harcama Analizi:** Chart.js ile görselleştirilmiş harcama dağılımı grafiği.
* 📜 **İşlem Geçmişi:** Yapılan tüm transferlerin detaylı listesi ve arama filtresi.
* 🌓 **Karanlık/Aydınlık Mod:** Kullanıcı tercihine göre değişen modern arayüz.
* 🌐 **Çoklu Dil Desteği:** Türkçe ve İngilizce dil seçenekleri.

## 🛠️ Kullanılan Teknolojiler

### Backend
* **Java 21**
* **Spring Boot 4.0.5**
* **Spring Data JPA**
* **PostgreSQL** (Veritabanı)
* **Lombok**

### Frontend
* **HTML5 & CSS3** (Modern ve Responsive tasarım)
* **Vanilla JavaScript**
* **Chart.js** (Grafikler için)
* **SweetAlert2** (Şık bildirimler için)

## 🚀 Kurulum ve Çalıştırma

1.  **Veritabanı Ayarları:**
    * PostgreSQL'de `p2p_db` isimli bir veritabanı oluşturun.
    * `src/main/resources/application.properties` dosyasındaki kullanıcı adı ve şifreyi kendi yerel ayarlarınıza göre güncelleyin.

2.  **Projeyi Çalıştırma:**
    ```bash
    ./mvnw spring-boot:run
    ```

3.  **Tarayıcıdan Erişim:**
    `http://localhost:8080` adresine gidin.
