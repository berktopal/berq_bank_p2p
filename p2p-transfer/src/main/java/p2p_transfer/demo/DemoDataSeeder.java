package p2p_transfer.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import p2p_transfer.account.Account;
import p2p_transfer.account.AccountRepository;
import p2p_transfer.budget.Budget;
import p2p_transfer.budget.BudgetRepository;
import p2p_transfer.common.Iban;
import p2p_transfer.config.AppProperties;
import p2p_transfer.contact.Contact;
import p2p_transfer.contact.ContactRepository;
import p2p_transfer.notification.NotificationService;
import p2p_transfer.notification.NotificationType;
import p2p_transfer.request.PaymentRequestDtos.CreatePaymentRequest;
import p2p_transfer.request.PaymentRequestService;
import p2p_transfer.schedule.ScheduledTransfer.Frequency;
import p2p_transfer.schedule.ScheduledTransferDtos.CreateScheduleRequest;
import p2p_transfer.schedule.ScheduledTransferService;
import p2p_transfer.transfer.ReferenceGenerator;
import p2p_transfer.transfer.Transaction;
import p2p_transfer.transfer.TransactionCategory;
import p2p_transfer.transfer.TransactionRepository;
import p2p_transfer.transfer.TransactionStatus;
import p2p_transfer.user.User;
import p2p_transfer.user.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 * Demo profilinde örnek veri yükler: giriş bilgisi {@value #DEMO_EMAIL} / {@value #DEMO_PASSWORD}.
 * Rastgelelik sabit tohumla üretilir; demo kullanıcısı zaten varsa hiçbir şey yapmaz.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.demo.seed", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_EMAIL = "demo@berqbank.dev";
    public static final String DEMO_PASSWORD = "Demo1234";

    private final UserRepository users;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final ContactRepository contacts;
    private final PasswordEncoder passwordEncoder;
    private final ReferenceGenerator references;
    private final AppProperties props;
    private final Clock clock;
    private final BudgetRepository budgets;
    private final PaymentRequestService paymentRequests;
    private final ScheduledTransferService schedules;
    private final NotificationService notifications;
    private static final Locale TURKISH = Locale.of("tr");

    private final Random random = new Random(42);

    public DemoDataSeeder(UserRepository users, AccountRepository accounts, TransactionRepository transactions,
                          ContactRepository contacts, PasswordEncoder passwordEncoder, ReferenceGenerator references,
                          AppProperties props, Clock clock, BudgetRepository budgets,
                          PaymentRequestService paymentRequests, ScheduledTransferService schedules,
                          NotificationService notifications) {
        this.users = users;
        this.accounts = accounts;
        this.transactions = transactions;
        this.contacts = contacts;
        this.passwordEncoder = passwordEncoder;
        this.references = references;
        this.props = props;
        this.clock = clock;
        this.budgets = budgets;
        this.paymentRequests = paymentRequests;
        this.schedules = schedules;
        this.notifications = notifications;
    }

    private record Planned(Instant at, Account from, Account to, BigDecimal amount, TransactionCategory category, String description) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            return;
        }
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        User deniz = user("Deniz", "Yılmaz", DEMO_EMAIL, hash);
        User ayse = user("Ayşe", "Kaya", "ayse@berqbank.dev", hash);
        User mehmet = user("Mehmet", "Demir", "mehmet@berqbank.dev", hash);
        User zeynep = user("Zeynep", "Çelik", "zeynep@berqbank.dev", hash);
        User can = user("Can", "Öztürk", "can@berqbank.dev", hash);

        Account main = account(deniz, "Vadesiz TL Hesabı", "TRY", "18500.00");
        Account savings = account(deniz, "Birikim Hesabı", "TRY", "42000.00");
        account(deniz, "Dolar Hesabı", "USD", "1250.00");
        Account ayseAcc = account(ayse, "Vadesiz TL Hesabı", "TRY", "36000.00");
        Account mehmetAcc = account(mehmet, "Vadesiz TL Hesabı", "TRY", "52000.00");
        Account zeynepAcc = account(zeynep, "Vadesiz TL Hesabı", "TRY", "24000.00");
        Account canAcc = account(can, "Ticari Hesap", "TRY", "400000.00");

        ZoneId zone = props.bank().zone();
        Instant now = clock.instant();
        YearMonth current = YearMonth.now(clock.withZone(zone));
        List<Planned> plan = new ArrayList<>();

        for (int back = 5; back >= 0; back--) {
            YearMonth m = current.minusMonths(back);
            String month = m.getMonth().getDisplayName(TextStyle.FULL, TURKISH);
            plan.add(at(m, 1, 10, zone, canAcc, main, amount(52000, 52000), TransactionCategory.GENERAL, "Proje ödemesi - " + month));
            plan.add(at(m, 3, 9, zone, main, mehmetAcc, amount(17500, 17500), TransactionCategory.RENT, "Kira - " + month));
            plan.add(at(m, 5, 20, zone, main, savings, amount(6000, 6000), TransactionCategory.SAVINGS, "Aylık birikim"));
            plan.add(at(m, 8, 13, zone, main, zeynepAcc, amount(900, 1600), TransactionCategory.BILLS, "Ortak fatura payı"));
            plan.add(at(m, 11, 21, zone, main, ayseAcc, amount(450, 1200), TransactionCategory.FOOD, "Akşam yemeği"));
            plan.add(at(m, 14, 18, zone, ayseAcc, main, amount(300, 900), TransactionCategory.FOOD, "Yemek hesabı"));
            plan.add(at(m, 17, 12, zone, main, zeynepAcc, amount(1200, 4800), TransactionCategory.SHOPPING, "Ortak alışveriş"));
            plan.add(at(m, 20, 8, zone, main, canAcc, amount(400, 900), TransactionCategory.TRANSPORT, "Yol paylaşımı"));
            plan.add(at(m, 23, 19, zone, main, ayseAcc, amount(600, 2500), TransactionCategory.ENTERTAINMENT, "Konser bileti"));
            if (back % 2 == 0) {
                plan.add(at(m, 26, 15, zone, main, mehmetAcc, amount(1500, 3500), TransactionCategory.HEALTH, "Diş kontrolü"));
                plan.add(at(m, 27, 11, zone, zeynepAcc, main, amount(1000, 2000), TransactionCategory.FAMILY, "Doğum günü hediyesi"));
            }
        }

        List<Transaction> applied = plan.stream()
                .filter(p -> p.at().isBefore(now.minus(Duration.ofMinutes(5))))
                .sorted(Comparator.comparing(Planned::at))
                .map(this::apply)
                .filter(Objects::nonNull)
                .toList();

        // Son gelen iki transfer için bildirim: zil ikonu ilk girişte boş görünmesin
        List<Transaction> incoming = applied.stream()
                .filter(t -> t.getReceiverAccount() == main && t.getSenderAccount().getUser() != deniz)
                .toList();
        incoming.subList(Math.max(0, incoming.size() - 2), incoming.size())
                .forEach(t -> notifications.notify(deniz.getId(), NotificationType.MONEY_RECEIVED, n -> {
                    n.setAmount(t.getAmount());
                    n.setCurrency(t.getCurrency());
                    n.setCounterpartyName(t.getSenderAccount().getUser().fullName());
                    n.setCategory(t.getCategory());
                    n.setTransactionId(t.getId());
                }));

        budget(deniz, TransactionCategory.FOOD, "2000");
        budget(deniz, TransactionCategory.SHOPPING, "4000");
        budget(deniz, TransactionCategory.ENTERTAINMENT, "1500");

        // Bekleyen istekler: biri Deniz'e gelen, biri Deniz'in gönderdiği
        paymentRequests.create(ayse.getId(), new CreatePaymentRequest(ayseAcc.getId(), main.getIban(),
                new BigDecimal("250.00"), "Konser bileti payın"));
        paymentRequests.create(deniz.getId(), new CreatePaymentRequest(main.getId(), zeynepAcc.getIban(),
                new BigDecimal("480.00"), "Ortak market alışverişi"));

        // Talimatlar: aylık kira ve haftalık birikim
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate rentDay = current.plusMonths(1).atDay(3);
        schedules.create(deniz.getId(), new CreateScheduleRequest(main.getId(), mehmetAcc.getIban(),
                new BigDecimal("17500.00"), "Kira", TransactionCategory.RENT, Frequency.MONTHLY, rentDay, null));
        schedules.create(deniz.getId(), new CreateScheduleRequest(main.getId(), savings.getIban(),
                new BigDecimal("1000.00"), "Haftalık birikim", TransactionCategory.SAVINGS, Frequency.WEEKLY,
                today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)), null));

        contact(deniz, ayseAcc, "Ayşe");
        contact(deniz, mehmetAcc, "Ev sahibi");
        contact(deniz, zeynepAcc, "Zeynep");
        log.info("Demo verisi yüklendi. Giriş: {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
    }

    private Transaction apply(Planned p) {
        Account from = p.from();
        Account to = p.to();
        if (from.getBalance().compareTo(p.amount()) < 0) {
            return null;
        }
        from.setBalance(from.getBalance().subtract(p.amount()));
        to.setBalance(to.getBalance().add(p.amount()));
        Transaction t = new Transaction();
        t.setReference(references.next());
        t.setSenderAccount(from);
        t.setReceiverAccount(to);
        t.setAmount(p.amount());
        t.setCurrency(from.getCurrency());
        t.setDescription(p.description());
        t.setCategory(p.category());
        t.setCreatedAt(p.at());
        t.setStatus(TransactionStatus.SUCCESS);
        t.setSenderBalanceAfter(from.getBalance());
        t.setReceiverBalanceAfter(to.getBalance());
        return transactions.save(t);
    }

    private void budget(User owner, TransactionCategory category, String limit) {
        Budget b = new Budget();
        b.setUserId(owner.getId());
        b.setCategory(category);
        b.setCurrency("TRY");
        b.setMonthlyLimit(new BigDecimal(limit));
        b.setCreatedAt(clock.instant());
        budgets.save(b);
    }

    private Planned at(YearMonth month, int day, int hour, ZoneId zone, Account from, Account to,
                       BigDecimal amount, TransactionCategory category, String description) {
        LocalDate date = month.atDay(Math.min(day, month.lengthOfMonth()));
        Instant at = date.atTime(hour, random.nextInt(60)).atZone(zone).toInstant();
        return new Planned(at, from, to, amount, category, description);
    }

    private BigDecimal amount(int min, int max) {
        // Sabit tutarlar (kira, maaş) kuruşsuz; değişkenler gerçekçi olsun diye kuruşlu
        if (min == max) {
            return BigDecimal.valueOf(min).setScale(2, RoundingMode.UNNECESSARY);
        }
        int value = min + random.nextInt(max - min);
        return BigDecimal.valueOf(value).add(BigDecimal.valueOf(random.nextInt(100), 2)).setScale(2, RoundingMode.UNNECESSARY);
    }

    private User user(String first, String last, String email, String hash) {
        User u = new User();
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(email);
        u.setTckn(randomTckn(random));
        u.setPassword(hash);
        u.setCreatedAt(clock.instant().minus(Duration.ofDays(200)));
        return users.save(u);
    }

    private Account account(User owner, String name, String currency, String balance) {
        Account a = new Account();
        a.setUser(owner);
        a.setName(name);
        a.setCurrency(currency);
        a.setIban(Iban.generate(props.bank().code()));
        a.setBalance(new BigDecimal(balance));
        a.setCreatedAt(clock.instant().minus(Duration.ofDays(200)));
        return accounts.save(a);
    }

    private void contact(User owner, Account account, String nickname) {
        Contact c = new Contact();
        c.setOwner(owner);
        c.setAccount(account);
        c.setNickname(nickname);
        c.setCreatedAt(clock.instant());
        contacts.save(c);
    }

    /** Algoritmaya uygun (ama gerçek bir kişiye ait olması beklenmeyen) T.C. kimlik numarası üretir. */
    public static String randomTckn(Random random) {
        int[] d = new int[11];
        d[0] = 1 + random.nextInt(9);
        for (int i = 1; i < 9; i++) {
            d[i] = random.nextInt(10);
        }
        int odd = d[0] + d[2] + d[4] + d[6] + d[8];
        int even = d[1] + d[3] + d[5] + d[7];
        d[9] = Math.floorMod(odd * 7 - even, 10);
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += d[i];
        }
        d[10] = sum % 10;
        StringBuilder sb = new StringBuilder();
        for (int x : d) {
            sb.append(x);
        }
        return sb.toString();
    }
}
