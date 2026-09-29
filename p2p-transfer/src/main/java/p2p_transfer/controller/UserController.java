package p2p_transfer.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import p2p_transfer.dto.ApiViews.UserView;
import p2p_transfer.entity.User;
import p2p_transfer.security.SessionAuth;
import p2p_transfer.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Kullanıcı Kaydı (Register) — yanıtta şifre/TCKN dönmez
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserView createUser(@RequestBody User user) {
        return UserView.of(userService.saveUser(user));
    }

    // Giriş: başarılıysa sunucu tarafında oturum açılır
    @PostMapping("/login")
    public UserView login(@RequestBody Map<String, String> credentials, HttpServletRequest request) {
        User user = userService.login(credentials.get("email"), credentials.get("password"));
        SessionAuth.signIn(request, user.getId());
        return UserView.of(user);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        SessionAuth.signOut(request);
    }
}
