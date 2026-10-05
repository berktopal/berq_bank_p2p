package p2p_transfer.transfer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import p2p_transfer.account.AccountService;
import p2p_transfer.common.PageResponse;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.config.AppProperties;
import p2p_transfer.transfer.TransferDtos.TransactionFilter;
import p2p_transfer.transfer.TransferDtos.TransactionResponse;

import java.util.List;

@Service
public class TransactionQueryService {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_EXPORT_ROWS = 5_000;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final AppProperties props;

    public TransactionQueryService(TransactionRepository transactionRepository, AccountService accountService,
                                   AppProperties props) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(Long userId, TransactionFilter filter, int page, int size) {
        assertAccountOwnership(userId, filter);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), NEWEST_FIRST);
        Page<Transaction> result = transactionRepository.findAll(
                TransactionSpecifications.forUser(userId, filter, props.bank().zone()), pageable);
        return PageResponse.of(result, t -> TransactionResponse.of(t, userId, filter.accountId()));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> export(Long userId, TransactionFilter filter) {
        assertAccountOwnership(userId, filter);
        Pageable pageable = PageRequest.of(0, MAX_EXPORT_ROWS, NEWEST_FIRST);
        return transactionRepository.findAll(TransactionSpecifications.forUser(userId, filter, props.bank().zone()), pageable)
                .map(t -> TransactionResponse.of(t, userId, filter.accountId()))
                .getContent();
    }

    /** @param perspectiveAccountId işlemin hangi hesabın gözünden gösterileceği; {@code null} olabilir */
    @Transactional(readOnly = true)
    public TransactionResponse get(Long userId, Long transactionId, Long perspectiveAccountId) {
        return transactionRepository.findForParticipant(transactionId, userId)
                .map(t -> TransactionResponse.of(t, userId, perspectiveAccountId))
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
    }

    private void assertAccountOwnership(Long userId, TransactionFilter filter) {
        if (filter.accountId() != null) {
            accountService.getOwned(userId, filter.accountId());
        }
    }
}
