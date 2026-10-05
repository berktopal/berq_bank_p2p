package p2p_transfer.transfer;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import p2p_transfer.account.Account;
import p2p_transfer.transfer.TransferDtos.Direction;
import p2p_transfer.transfer.TransferDtos.TransactionFilter;
import p2p_transfer.user.User;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** İşlem listesi filtreleri. Kullanıcının taraf olmadığı işlemler hiçbir filtre kombinasyonunda dönmez. */
final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    static Specification<Transaction> forUser(Long userId, TransactionFilter f, ZoneId zone) {
        return (root, query, cb) -> {
            Join<Transaction, Account> sender = root.join("senderAccount", JoinType.INNER);
            Join<Transaction, Account> receiver = root.join("receiverAccount", JoinType.INNER);
            Join<Account, User> senderUser = sender.join("user", JoinType.INNER);
            Join<Account, User> receiverUser = receiver.join("user", JoinType.INNER);

            List<Predicate> p = new ArrayList<>();
            p.add(cb.or(cb.equal(senderUser.get("id"), userId), cb.equal(receiverUser.get("id"), userId)));

            if (f.accountId() != null) {
                if (f.direction() == Direction.OUTGOING) {
                    p.add(cb.equal(sender.get("id"), f.accountId()));
                } else if (f.direction() == Direction.INCOMING) {
                    p.add(cb.equal(receiver.get("id"), f.accountId()));
                } else {
                    p.add(cb.or(cb.equal(sender.get("id"), f.accountId()), cb.equal(receiver.get("id"), f.accountId())));
                }
            } else if (f.direction() == Direction.OUTGOING) {
                p.add(cb.equal(senderUser.get("id"), userId));
            } else if (f.direction() == Direction.INCOMING) {
                p.add(cb.equal(receiverUser.get("id"), userId));
            }

            if (f.from() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from().atStartOfDay(zone).toInstant()));
            }
            if (f.to() != null) {
                p.add(cb.lessThan(root.get("createdAt"), f.to().plusDays(1).atStartOfDay(zone).toInstant()));
            }
            if (f.category() != null) {
                p.add(cb.equal(root.get("category"), f.category()));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + escapeLike(f.q().trim().toLowerCase(Locale.ROOT)) + "%";
                String ibanLike = "%" + escapeLike(f.q().replaceAll("\\s+", "").toUpperCase(Locale.ROOT)) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("description")), like, '\\'),
                        cb.like(cb.lower(root.get("reference")), like, '\\'),
                        cb.like(sender.get("iban"), ibanLike, '\\'),
                        cb.like(receiver.get("iban"), ibanLike, '\\'),
                        cb.like(cb.lower(cb.concat(cb.concat(senderUser.get("firstName"), " "), senderUser.get("lastName"))), like, '\\'),
                        cb.like(cb.lower(cb.concat(cb.concat(receiverUser.get("firstName"), " "), receiverUser.get("lastName"))), like, '\\')));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
