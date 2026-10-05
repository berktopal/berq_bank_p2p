package p2p_transfer.transfer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import p2p_transfer.account.Account;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.support.IntegrationTest;
import p2p_transfer.transfer.TransferDtos.TransferRequest;
import p2p_transfer.user.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gerçek PostgreSQL üzerinde, aynı anda başlayan transferlerle:
 * double spend olmadığını, deadlock oluşmadığını ve toplam paranın korunduğunu doğrular.
 */
class ConcurrentTransferTests extends IntegrationTest {

    private static final int THREADS = 20;

    @Autowired
    TransferService transferService;

    @Test
    void parallelTransfersNeverOverdrawTheSender() throws Exception {
        User alice = data.user("Alice");
        User bob = data.user("Bob");
        Account from = data.account(alice, "100.00");
        Account to = data.account(bob, "0.00");

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        runConcurrently(THREADS, i -> () -> {
            try {
                transferService.transfer(alice.getId(), request(from, to, "10.00"), null);
                succeeded.incrementAndGet();
            } catch (BusinessException e) {
                assertThat(e.code()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
                rejected.incrementAndGet();
            }
            return null;
        });

        assertThat(succeeded).hasValue(10);
        assertThat(rejected).hasValue(10);
        assertThat(data.balance(from)).isEqualByComparingTo("0.00");
        assertThat(data.balance(to)).isEqualByComparingTo("100.00");
        assertThat(jdbc.queryForObject("select count(*) from transactions", Long.class)).isEqualTo(10);
    }

    @Test
    void opposingTransfersDoNotDeadlockAndConserveMoney() throws Exception {
        User alice = data.user("Alice");
        User bob = data.user("Bob");
        Account a = data.account(alice, "1000.00");
        Account b = data.account(bob, "1000.00");

        runConcurrently(THREADS * 2, i -> () -> {
            if (i % 2 == 0) {
                transferService.transfer(alice.getId(), request(a, b, "7.00"), null);
            } else {
                transferService.transfer(bob.getId(), request(b, a, "3.00"), null);
            }
            return null;
        });

        // A: 20 × (−7 + 3) = −80, B: +80; toplam 2000 olarak kalır
        assertThat(data.balance(a)).isEqualByComparingTo("920.00");
        assertThat(data.balance(b)).isEqualByComparingTo("1080.00");
    }

    @Test
    void concurrentRetriesWithSameIdempotencyKeyChargeOnce() throws Exception {
        User alice = data.user("Alice");
        User bob = data.user("Bob");
        Account from = data.account(alice, "500.00");
        Account to = data.account(bob, "0.00");

        runConcurrently(10, i -> () -> transferService.transfer(alice.getId(), request(from, to, "50.00"), "same-key-123"));

        assertThat(data.balance(from)).isEqualByComparingTo("450.00");
        assertThat(jdbc.queryForObject("select count(*) from transactions", Long.class)).isEqualTo(1);
    }

    private static TransferRequest request(Account from, Account to, String amount) {
        return new TransferRequest(from.getId(), to.getIban(), new BigDecimal(amount), null, null);
    }

    private interface TaskFactory {
        Callable<Object> create(int index);
    }

    /** Tüm görevleri bir kapıda bekletip aynı anda serbest bırakır; herhangi bir beklenmeyen hata testi düşürür. */
    private static void runConcurrently(int count, TaskFactory factory) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Callable<Object> task = factory.create(i);
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            for (Future<Object> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
