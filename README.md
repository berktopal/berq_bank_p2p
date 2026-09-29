#  Berq Bank - P2P Money Transfer System

Berq Bank is a modern digital banking application architected to allow users to securely log in, monitor account balances, and execute instant money transfers via IBAN.

##  Features

*  **Secure Login System:** User-friendly, in-page error notifications for incorrect password attempts.
*  **P2P Money Transfer:** Instant money transfers via IBAN and balance updates.
*  **Expense Analysis:** Expense distribution charts visualized with Chart.js.
*  **Transaction History:** Detailed list of all executed transfers and a search filter.
*  **Dark/Light Mode:** A modern interface that adapts to user preferences.
*  **Multi-Language Support:** Turkish and English language options.

##  Security

* **Server-side sessions:** Login creates an HttpOnly, SameSite=Strict session cookie; every account and transfer endpoint requires it.
* **Ownership checks:** Users can only view their own accounts/transactions and can only send money from accounts they own.
* **Transfer validation:** Amounts must be positive with at most 2 decimals; same-currency only; balance rows are locked (`PESSIMISTIC_WRITE`) to prevent double spending.
* **Password hashing:** Passwords are stored as BCrypt hashes (legacy plaintext records are upgraded on next login).
* **Data minimization:** API responses use DTOs, so passwords, TCKN and other users' balances are never exposed; IBAN lookup returns a masked name.
* **XSS protection:** User-provided values are escaped before being rendered in the dashboard.

##  Technologies Used

### Backend
* **Java 21**
* **Spring Boot 4.0.5**
* **Spring Data JPA**
* **PostgreSQL** (Database)

### Frontend
* **HTML5 & CSS3** (Modern and Responsive design)
* **Vanilla JavaScript**
* **Chart.js** (For charts)
* **SweetAlert2** (For elegant notifications)

##  Setup and Execution

1.  **Database Configuration:**
    * Create a database named `p2p_db` in PostgreSQL.
    * Set your database password as an environment variable (never commit it): `export DB_PASSWORD=your_password`.
    * If needed, update the username in `src/main/resources/application.properties`.

2.  **Running the Project:**
    ```bash
    ./mvnw spring-boot:run
    ```

3.  **Browser Access:**
    Navigate to `http://localhost:8080`.
