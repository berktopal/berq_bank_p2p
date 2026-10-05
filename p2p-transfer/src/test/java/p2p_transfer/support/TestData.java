package p2p_transfer.support;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import p2p_transfer.account.Account;
import p2p_transfer.account.AccountRepository;
import p2p_transfer.auth.AuthUser;
import p2p_transfer.common.Iban;
import p2p_transfer.demo.DemoDataSeeder;
import p2p_transfer.user.User;
import p2p_transfer.user.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Random;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/** Testler için kısa yoldan kullanıcı/hesap oluşturma ve oturum taklidi. */
@TestComponent
public class TestData {

    public static final String PASSWORD = "Password123";

    private final UserRepository users;
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();
    private String passwordHash;

    public TestData(UserRepository users, AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    public User user(String firstName) {
        if (passwordHash == null) {
            passwordHash = passwordEncoder.encode(PASSWORD);
        }
        User u = new User();
        u.setFirstName(firstName);
        u.setLastName("Test");
        u.setEmail(firstName.toLowerCase(Locale.ROOT) + "@test.local");
        u.setTckn(DemoDataSeeder.randomTckn(random));
        u.setPassword(passwordHash);
        u.setCreatedAt(Instant.now());
        return users.save(u);
    }

    public Account account(User owner, String balance) {
        return account(owner, balance, "TRY");
    }

    public Account account(User owner, String balance, String currency) {
        Account a = new Account();
        a.setUser(owner);
        a.setName("Test " + currency);
        a.setIban(Iban.generate("00999"));
        a.setBalance(new BigDecimal(balance));
        a.setCurrency(currency);
        a.setCreatedAt(Instant.now());
        return accounts.save(a);
    }

    public BigDecimal balance(Account account) {
        return accounts.findById(account.getId()).orElseThrow().getBalance();
    }

    /** İsteği verilen kullanıcının oturumu açıkmış gibi çalıştırır. */
    public static RequestPostProcessor as(User user) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthUser(user.getId(), user.getEmail()), null, AuthorityUtils.createAuthorityList("ROLE_USER")));
    }
}
