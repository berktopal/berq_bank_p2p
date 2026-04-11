package p2p_transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import p2p_transfer.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    
    // --- YENİ EKLENEN: GİRİŞ İÇİN E-POSTA İLE BULMA ---
    Optional<User> findByEmail(String email);
}