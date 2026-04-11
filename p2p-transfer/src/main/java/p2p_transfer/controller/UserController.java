package p2p_transfer.controller;

import org.springframework.web.bind.annotation.*;
import p2p_transfer.entity.User;
import p2p_transfer.service.UserService;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Kullanıcı Kaydı (Register)
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.saveUser(user);
    }

    // --- YENİ EKLENEN: GİRİŞ YAPMA (LOGIN) ---
    @PostMapping("/login")
    public User login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        return userService.login(email, password);
    }
}