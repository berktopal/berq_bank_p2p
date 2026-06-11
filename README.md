#  Berq Bank - P2P Money Transfer System

Berq Bank is a modern digital banking application architected to allow users to securely log in, monitor account balances, and execute instant money transfers via IBAN.

##  Features

**Secure Login System:** User-friendly, in-page error notifications for incorrect password attempts.
**P2P Money Transfer:** Instant money transfers via IBAN and balance updates.
**Expense Analysis:** Expense distribution charts visualized with Chart.js.
**Transaction History:** Detailed list of all executed transfers and a search filter.
**Dark/Light Mode:** A modern interface that adapts to user preferences.
**Multi-Language Support:** Turkish and English language options.

##  Technologies Used

### Backend
* **Java 21**
* **Spring Boot 4.0.5**
* **Spring Data JPA**
* **PostgreSQL** (Database)
* **Lombok**

### Frontend
* **HTML5 & CSS3** (Modern and Responsive design)
* **Vanilla JavaScript**
* **Chart.js** (For charts)
* **SweetAlert2** (For elegant notifications)

##  Setup and Execution

1.  **VDatabase Configuration:**
    * Create a database named `p2p_db` in PostgreSQL.
    * Update the username and password in the `src/main/resources/application.properties` file according to your local environment.

2.  **Running the Project:**
    ```bash
    ./mvnw spring-boot:run
    ```

3.  **Browser Access:**
    Navigate to `http://localhost:8080`.
