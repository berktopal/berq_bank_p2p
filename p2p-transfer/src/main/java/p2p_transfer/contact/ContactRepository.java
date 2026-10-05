package p2p_transfer.contact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    @Query("""
            select c from Contact c
            join fetch c.account a join fetch a.user
            where c.owner.id = :ownerId
            order by lower(c.nickname)
            """)
    List<Contact> findAllForOwner(@Param("ownerId") Long ownerId);

    @Query("""
            select c from Contact c
            join fetch c.account a join fetch a.user
            where c.id = :id and c.owner.id = :ownerId
            """)
    Optional<Contact> findForOwner(@Param("id") Long id, @Param("ownerId") Long ownerId);

    boolean existsByOwnerIdAndAccountId(Long ownerId, Long accountId);
}
