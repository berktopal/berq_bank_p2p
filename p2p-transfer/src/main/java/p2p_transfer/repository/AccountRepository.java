package p2p_transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import p2p_transfer.entity.Account;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);
    
    // IBAN ile hesap bulmak için (Yeni eklendi)
    Optional<Account> findByIban(String iban);
}