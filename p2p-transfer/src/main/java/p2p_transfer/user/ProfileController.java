package p2p_transfer.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import p2p_transfer.auth.AuthDtos.ChangePasswordRequest;
import p2p_transfer.auth.AuthService;
import p2p_transfer.auth.AuthUser;
import p2p_transfer.common.Masking;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;

import java.time.Instant;

@Tag(name = "Profile")
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final AuthService authService;

    public ProfileController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    public record ProfileResponse(Long id, String firstName, String lastName, String email, String maskedTckn, Instant createdAt) {
    }

    @GetMapping
    public ProfileResponse get(@AuthenticationPrincipal AuthUser me) {
        User u = userRepository.findById(me.id()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return new ProfileResponse(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(),
                Masking.maskTckn(u.getTckn()), u.getCreatedAt());
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(me.id(), request.currentPassword(), request.newPassword());
    }
}
