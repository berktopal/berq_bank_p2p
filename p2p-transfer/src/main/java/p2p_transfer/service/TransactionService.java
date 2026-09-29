package p2p_transfer.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import p2p_transfer.dto.ApiViews.TransactionView;
import p2p_transfer.entity.Account;
import p2p_transfer.entity.Transaction;
import p2p_transfer.entity.TransactionStatus;
import p2p_transfer.repository.AccountRepository;
import p2p_transfer.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    // Sadece giriş yapan kullanıcının taraf olduğu işlemler
    @Transactional(readOnly = true)
    public List<TransactionView> getTransactionsForUser(Long userId) {
        return transactionRepository.findAllForUser(userId).stream().map(TransactionView::of).toList();
    }

    @Transactional
    public TransactionView transferMoney(Long currentUserId, Long senderAccountId, Long receiverAccountId, BigDecimal amount) {
        // 0. Girdi doğrulama
        if (senderAccountId == null || receiverAccountId == null || amount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gönderen hesap, alıcı hesap ve tutar zorunludur.");
        }
        // Negatif veya sıfır tutar: aksi halde alıcıdan para çekilebilirdi
        if (amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer tutarı sıfırdan büyük olmalıdır.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tutar en fazla 2 ondalık basamak içerebilir.");
        }

        // 1. Aynı hesaba transferi engelle
        if (senderAccountId.equals(receiverAccountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kendi hesabınıza transfer yapamazsınız!");
        }

        // 2. Hesapları kilitleyerek bul (deadlock olmaması için her zaman küçük ID önce)
        Account sender;
        Account receiver;
        if (senderAccountId < receiverAccountId) {
            sender = lockAccount(senderAccountId, "Gönderen hesap bulunamadı!");
            receiver = lockAccount(receiverAccountId, "Alıcı hesap bulunamadı!");
        } else {
            receiver = lockAccount(receiverAccountId, "Alıcı hesap bulunamadı!");
            sender = lockAccount(senderAccountId, "Gönderen hesap bulunamadı!");
        }

        // 3. Yetki kontrolü: kullanıcı sadece KENDİ hesabından para gönderebilir
        if (!Objects.equals(sender.getUser().getId(), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu hesaptan transfer yapma yetkiniz yok.");
        }

        if (!Objects.equals(sender.getCurrency(), receiver.getCurrency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Farklı para birimindeki hesaplar arasında transfer yapılamaz.");
        }

        // 4. Bakiye Kontrolü
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yetersiz bakiye!");
        }

        // 5. Parayı Hareket Ettir
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        accountRepository.save(sender);
        accountRepository.save(receiver);

        // 6. Kayıt (Dekont) Oluştur
        Transaction transaction = new Transaction();
        transaction.setSenderAccount(sender);
        transaction.setReceiverAccount(receiver);
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.SUCCESS);

        return TransactionView.of(transactionRepository.save(transaction));
    }

    private Account lockAccount(Long id, String notFoundMessage) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, notFoundMessage));
    }
}
