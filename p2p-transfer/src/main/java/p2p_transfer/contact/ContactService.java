package p2p_transfer.contact;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import p2p_transfer.account.Account;
import p2p_transfer.account.AccountService;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.user.UserRepository;

import java.time.Clock;
import java.util.List;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final AccountService accountService;
    private final UserRepository userRepository;
    private final Clock clock;

    public ContactService(ContactRepository contactRepository, AccountService accountService,
                          UserRepository userRepository, Clock clock) {
        this.contactRepository = contactRepository;
        this.accountService = accountService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Contact> list(Long userId) {
        return contactRepository.findAllForOwner(userId);
    }

    @Transactional
    public Contact create(Long userId, String nickname, String iban) {
        Account account = accountService.lookupByIban(iban);
        if (account.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.CONTACT_IS_SELF);
        }
        if (contactRepository.existsByOwnerIdAndAccountId(userId, account.getId())) {
            throw new BusinessException(ErrorCode.CONTACT_EXISTS);
        }
        Contact contact = new Contact();
        contact.setOwner(userRepository.getReferenceById(userId));
        contact.setAccount(account);
        contact.setNickname(nickname.trim());
        contact.setCreatedAt(clock.instant());
        return contactRepository.save(contact);
    }

    @Transactional
    public Contact rename(Long userId, Long contactId, String nickname) {
        Contact contact = getOwned(userId, contactId);
        contact.setNickname(nickname.trim());
        return contact;
    }

    @Transactional
    public void delete(Long userId, Long contactId) {
        contactRepository.delete(getOwned(userId, contactId));
    }

    private Contact getOwned(Long userId, Long contactId) {
        return contactRepository.findForOwner(contactId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTACT_NOT_FOUND));
    }
}
