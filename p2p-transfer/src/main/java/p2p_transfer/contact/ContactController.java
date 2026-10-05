package p2p_transfer.contact;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import p2p_transfer.auth.AuthUser;
import p2p_transfer.common.Masking;
import p2p_transfer.user.User;

import java.time.Instant;
import java.util.List;

@Tag(name = "Contacts")
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    public record CreateContactRequest(@NotBlank @Size(max = 40) String nickname, @NotBlank String iban) {
    }

    public record RenameContactRequest(@NotBlank @Size(max = 40) String nickname) {
    }

    public record ContactResponse(Long id, String nickname, String iban, String ownerName, String currency, Instant createdAt) {
        static ContactResponse of(Contact c) {
            User u = c.getAccount().getUser();
            return new ContactResponse(c.getId(), c.getNickname(), c.getAccount().getIban(),
                    Masking.maskedName(u.getFirstName(), u.getLastName()), c.getAccount().getCurrency(), c.getCreatedAt());
        }
    }

    @GetMapping
    public List<ContactResponse> list(@AuthenticationPrincipal AuthUser me) {
        return contactService.list(me.id()).stream().map(ContactResponse::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateContactRequest request) {
        return ContactResponse.of(contactService.create(me.id(), request.nickname(), request.iban()));
    }

    @PatchMapping("/{id}")
    public ContactResponse rename(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                  @Valid @RequestBody RenameContactRequest request) {
        return ContactResponse.of(contactService.rename(me.id(), id, request.nickname()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        contactService.delete(me.id(), id);
    }
}
