package p2p_transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Kimlik doğrulama AuthService'te yapılır; Spring'in varsayılan bellek içi kullanıcısı (rastgele şifreli) gereksiz
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class P2pTransferApplication {

	public static void main(String[] args) {
		SpringApplication.run(P2pTransferApplication.class, args);
	}

}
