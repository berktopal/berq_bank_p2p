package p2p_transfer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import p2p_transfer.entity.Account;
import p2p_transfer.entity.Transaction;
import p2p_transfer.entity.TransactionStatus;
import p2p_transfer.repository.AccountRepository;
import p2p_transfer.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List; // Listeleme için bu import şart!

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    // --- YENİ EKLENEN LİSTELEME METODU ---
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Transactional
    public Transaction transferMoney(Long senderAccountId, Long receiverAccountId, BigDecimal amount) {
        // 1. Aynı hesaba transferi engelle
        if (senderAccountId.equals(receiverAccountId)) {
            throw new RuntimeException("Kendi hesabınıza transfer yapamazsınız!");
        }

        // 2. Hesapları Bul
        Account sender = accountRepository.findById(senderAccountId)
                .orElseThrow(() -> new RuntimeException("Gönderen hesap bulunamadı!"));
        Account receiver = accountRepository.findById(receiverAccountId)
                .orElseThrow(() -> new RuntimeException("Alıcı hesap bulunamadı!"));

        // 3. Bakiye Kontrolü
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Yetersiz bakiye! Mevcut bakiyeniz: " + sender.getBalance());
        }

        // 4. Parayı Hareket Ettir
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        // 5. Veritabanını Güncelle
        accountRepository.save(sender);
        accountRepository.save(receiver);

        // 6. Kayıt (Dekont) Oluştur
        Transaction transaction = new Transaction();
        transaction.setSenderAccount(sender);
        transaction.setReceiverAccount(receiver);
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.SUCCESS);

        return transactionRepository.save(transaction);
    }
}