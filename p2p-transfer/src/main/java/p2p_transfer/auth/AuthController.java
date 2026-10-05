package p2p_transfer.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import p2p_transfer.auth.AuthDtos.CsrfResponse;
import p2p_transfer.auth.AuthDtos.LoginRequest;
import p2p_transfer.auth.AuthDtos.RegisterRequest;
import p2p_transfer.auth.AuthDtos.UserResponse;
import p2p_transfer.common.error.BusinessException;
import p2p_transfer.common.error.ErrorCode;
import p2p_transfer.user.User;
import p2p_transfer.user.UserRepository;

/** Oturum açma/kapama. Çıkış ({@code POST /api/auth/logout}) Spring Security'nin logout filtresiyle yapılır. */
@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository;
    private final SessionRegistry sessionRegistry;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public AuthController(AuthService authService, UserRepository userRepository,
                          SecurityContextRepository securityContextRepository, SessionRegistry sessionRegistry) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.securityContextRepository = securityContextRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @Operation(summary = "CSRF token'ını üretir ve XSRF-TOKEN çerezine yazar")
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest body,
                                 HttpServletRequest request, HttpServletResponse response) {
        User user = authService.register(body);
        signIn(user, request, response);
        return UserResponse.of(user);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest body,
                              HttpServletRequest request, HttpServletResponse response) {
        User user = authService.authenticate(body.email(), body.password());
        signIn(user, request, response);
        return UserResponse.of(user);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser me) {
        return userRepository.findById(me.id())
                .map(UserResponse::of)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
    }

    private void signIn(User user, HttpServletRequest request, HttpServletResponse response) {
        // Session fixation: girişten önce var olan oturumun kimliği değiştirilir
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        var principal = new AuthUser(user.getId(), user.getEmail());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        sessionRegistry.registerNewSession(request.getSession().getId(), principal);
    }
}
