package p2p_transfer.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import p2p_transfer.entity.Account;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);

    // IBAN ile hesap bulmak için
    Optional<Account> findByIban(String iban);

    // Transfer sırasında satırı kilitler: aynı anda gelen iki transfer bakiyeyi
    // ikisi de "yeterli" görüp eksiye düşüremez (race condition / double spend).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
