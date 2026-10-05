package p2p_transfer.schedule;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, Long> {

    @EntityGraph(attributePaths = {"fromAccount", "toAccount", "toAccount.user"})
    @Query("""
            select s from ScheduledTransfer s where s.userId = :userId
            order by case when s.status in (p2p_transfer.schedule.ScheduledTransfer.Status.ACTIVE,
                                            p2p_transfer.schedule.ScheduledTransfer.Status.PAUSED) then 0 else 1 end,
                     s.nextRunDate, s.id
            """)
    List<ScheduledTransfer> findAllForUser(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"fromAccount", "toAccount", "toAccount.user"})
    Optional<ScheduledTransfer> findByIdAndUserId(Long id, Long userId);

    /**
     * Vadesi gelmiş bir talimatı alıp kilitler. SKIP LOCKED: başka bir sunucu/iş parçacığı o satırı
     * işliyorsa beklemeden sıradakine geçilir; aynı talimat asla iki yerde aynı anda çalışmaz.
     */
    @Query(value = """
            select * from scheduled_transfers
            where status = 'ACTIVE' and next_run_date <= :today
            order by next_run_date, id
            limit 1
            for update skip locked
            """, nativeQuery = true)
    Optional<ScheduledTransfer> claimNextDue(@Param("today") LocalDate today);
}
