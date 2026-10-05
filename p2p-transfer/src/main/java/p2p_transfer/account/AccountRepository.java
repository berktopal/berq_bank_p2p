package p2p_transfer.account;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserIdOrderByIdAsc(Long userId);

    long countByUserId(Long userId);

    Optional<Account> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<Account> findByIban(String iban);

    boolean existsByIban(String iban);

    // Yalnızca id döner: alıcıyı çözmek için hesabın tamamını yüklemeye gerek yok
    @Query("select a.id from Account a where a.iban = :iban")
    Optional<Long> findIdByIban(@Param("iban") String iban);
}
