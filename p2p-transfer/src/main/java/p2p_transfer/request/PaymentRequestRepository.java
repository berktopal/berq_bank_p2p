package p2p_transfer.request;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {

    /**
     * role: IN → kullanıcı ödeyen (gelen istek), OUT → kullanıcı isteyen, ALL → ikisi.
     * (Null olabilen bir Boolean parametre PostgreSQL'de tür çıkarımı hatası verir; bu yüzden metin kullanılır.)
     */
    @EntityGraph(attributePaths = {"requester", "payer", "requesterAccount"})
    @Query("""
            select r from PaymentRequest r
            where ((:role = 'ALL' and (r.payer.id = :userId or r.requester.id = :userId))
                or (:role = 'IN' and r.payer.id = :userId)
                or (:role = 'OUT' and r.requester.id = :userId))
              and r.status in :statuses
            order by r.createdAt desc, r.id desc
            """)
    Page<PaymentRequest> findForUser(@Param("userId") Long userId, @Param("role") String role,
                                     @Param("statuses") Collection<PaymentRequest.Status> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"requester", "payer", "requesterAccount"})
    @Query("select r from PaymentRequest r where r.id = :id and (r.payer.id = :userId or r.requester.id = :userId)")
    Optional<PaymentRequest> findForParticipant(@Param("id") Long id, @Param("userId") Long userId);

    /** Ödeme/ret/iptal sırasında satırı kilitler: aynı istek iki kez ödenemez, ödenirken iptal edilemez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PaymentRequest r where r.id = :id")
    Optional<PaymentRequest> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select count(r) from PaymentRequest r
            where r.payer.id = :userId and r.status = p2p_transfer.request.PaymentRequest.Status.PENDING
              and r.expiresAt > :now
            """)
    long countPendingIncoming(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("""
            update PaymentRequest r set r.status = p2p_transfer.request.PaymentRequest.Status.EXPIRED
            where r.status = p2p_transfer.request.PaymentRequest.Status.PENDING and r.expiresAt < :now
            """)
    int expireOverdue(@Param("now") Instant now);
}
