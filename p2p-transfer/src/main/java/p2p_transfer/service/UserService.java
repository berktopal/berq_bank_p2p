package p2p_transfer.service;

import org.springframework.stereotype.Service;
import p2p_transfer.entity.User;
import p2p_transfer.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Kullanıcı Kaydı
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    // --- YENİ EKLENEN: LOGİN MANTIĞI ---
    public User login(String email, String password) {
        // 1. Veritabanında e-posta ile kullanıcıyı ara
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Bu e-posta adresiyle kayıtlı kullanıcı bulunamadı!"));

        // 2. Şifreyi kontrol et (Şu an düz metin olarak kontrol ediyoruz)
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Hatalı şifre! Lütfen tekrar deneyin.");
        }

        // 3. Her şey doğruysa kullanıcıyı döndür
        return user;
    }
}