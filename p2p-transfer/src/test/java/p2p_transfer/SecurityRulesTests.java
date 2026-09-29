package p2p_transfer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import p2p_transfer.entity.Account;
import p2p_transfer.entity.User;
import p2p_transfer.repository.AccountRepository;
import p2p_transfer.repository.UserRepository;
import p2p_transfer.service.TransactionService;
import p2p_transfer.service.UserService;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SecurityRulesTests {

    @Autowired UserService userService;
    @Autowired TransactionService transactionService;
    @Autowired UserRepository userRepository;
    @Autowired AccountRepository accountRepository;

    User alice;
    User bob;
    Account aliceAcc;
    Account bobAcc;

    @BeforeEach
    void setUp() {
        alice = userService.saveUser(newUser("Alice", "Password123"));
        bob = userService.saveUser(newUser("Bob", "Password456"));
        aliceAcc = newAccount(alice, "100.00");
        bobAcc = newAccount(bob, "100.00");
    }

    @Test
    void passwordsAreStoredAsBcryptHashes() {
        String stored = userRepository.findById(alice.getId()).orElseThrow().getPassword();
        assertNotEquals("Password123", stored);
        assertTrue(stored.startsWith("$2"));
        assertEquals(alice.getId(), userService.login(alice.getEmail(), "Password123").getId());
    }

    @Test
    void wrongPasswordIsRejected() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> userService.login(alice.getEmail(), "wrong-password"));
        assertEquals(401, e.getStatusCode().value());
    }

    @Test
    void legacyPlaintextPasswordIsUpgradedOnLogin() {
        User legacy = newUser("Legacy", "legacyPass1");
        legacy = userRepository.save(legacy); // eski sistem gibi düz metin kaydet
        userService.login(legacy.getEmail(), "legacyPass1");
        assertTrue(userRepository.findById(legacy.getId()).orElseThrow().getPassword().startsWith("$2"));
    }

    @Test
    void negativeAmountIsRejected() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> transactionService.transferMoney(alice.getId(), aliceAcc.getId(), bobAcc.getId(), new BigDecimal("-50")));
        assertEquals(400, e.getStatusCode().value());
        assertBalances("100.00", "100.00");
    }

    @Test
    void cannotSendFromSomeoneElsesAccount() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> transactionService.transferMoney(bob.getId(), aliceAcc.getId(), bobAcc.getId(), new BigDecimal("10")));
        assertEquals(403, e.getStatusCode().value());
        assertBalances("100.00", "100.00");
    }

    @Test
    void insufficientBalanceIsRejected() {
        assertThrows(ResponseStatusException.class,
                () -> transactionService.transferMoney(alice.getId(), aliceAcc.getId(), bobAcc.getId(), new BigDecimal("100.01")));
        assertBalances("100.00", "100.00");
    }

    @Test
    void validTransferMovesMoneyAndIsVisibleOnlyToParticipants() {
        transactionService.transferMoney(alice.getId(), aliceAcc.getId(), bobAcc.getId(), new BigDecimal("30.50"));
        assertBalances("69.50", "130.50");

        assertEquals(1, transactionService.getTransactionsForUser(alice.getId()).size());
        assertEquals(1, transactionService.getTransactionsForUser(bob.getId()).size());

        User carol = userService.saveUser(newUser("Carol", "Password789"));
        assertTrue(transactionService.getTransactionsForUser(carol.getId()).isEmpty());
    }

    private void assertBalances(String alice, String bob) {
        assertEquals(0, new BigDecimal(alice).compareTo(accountRepository.findById(aliceAcc.getId()).orElseThrow().getBalance()));
        assertEquals(0, new BigDecimal(bob).compareTo(accountRepository.findById(bobAcc.getId()).orElseThrow().getBalance()));
    }

    private static User newUser(String name, String password) {
        long n = ThreadLocalRandom.current().nextLong(10_000_000_000L, 99_999_999_999L);
        User u = new User();
        u.setFirstName(name);
        u.setLastName("Test");
        u.setTckn(Long.toString(n));
        u.setEmail(name.toLowerCase() + n + "@test.local");
        u.setPassword(password);
        return u;
    }

    private Account newAccount(User owner, String balance) {
        StringBuilder iban = new StringBuilder("TR");
        for (int i = 0; i < 24; i++) {
            iban.append(ThreadLocalRandom.current().nextInt(10));
        }
        Account a = new Account();
        a.setUser(owner);
        a.setIban(iban.toString());
        a.setBalance(new BigDecimal(balance));
        a.setCurrency("TRY");
        return accountRepository.save(a);
    }
}
