package p2p_transfer.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import p2p_transfer.entity.User;
import p2p_transfer.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class UserService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String INVALID_CREDENTIALS = "E-posta veya şifre hatalı.";

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Kullanıcı Kaydı: şifre veritabanına asla düz metin yazılmaz (BCrypt hash).
    @Transactional
    public User saveUser(User user) {
        String password = user.getPassword();
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Şifre en az " + MIN_PASSWORD_LENGTH + " karakter olmalıdır.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-posta zorunludur.");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu e-posta adresi zaten kayıtlı.");
        }
        user.setId(null); // istemcinin mevcut bir kaydı ezmesini engelle
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    // Login: kullanıcı var/yok bilgisi sızmasın diye tek tip hata mesajı döner.
    @Transactional
    public User login(String email, String password) {
        if (email == null || password == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS));

        String stored = user.getPassword();
        boolean valid;
        if (isBcryptHash(stored)) {
            valid = passwordEncoder.matches(password, stored);
        } else {
            // Eski (düz metin) kayıtlar için geriye dönük uyumluluk:
            // doğru girilirse şifre hemen BCrypt'e yükseltilir.
            valid = stored != null && MessageDigest.isEqual(
                    stored.getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8));
            if (valid) {
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
            }
        }

        if (!valid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        return user;
    }

    private static boolean isBcryptHash(String value) {
        return value != null && value.matches("^\\$2[aby]?\\$\\d{2}\\$.{53}$");
    }
}
