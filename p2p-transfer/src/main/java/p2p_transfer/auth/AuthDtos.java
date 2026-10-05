package p2p_transfer.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import p2p_transfer.common.validation.Tckn;
import p2p_transfer.user.User;

public final class AuthDtos {

    /** En az 8 karakter, en az bir harf ve bir rakam. BCrypt 72 bayttan sonrasını yok saydığı için üst sınır var. */
    static final String PASSWORD_PATTERN = "^(?=.*\\p{L})(?=.*\\d).{8,72}$";
    static final String PASSWORD_MESSAGE = "Şifre 8-72 karakter olmalı, en az bir harf ve bir rakam içermelidir.";
    private static final String NAME_PATTERN = "^[\\p{L}][\\p{L} .'-]*$";

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 50) @Pattern(regexp = NAME_PATTERN, message = "Geçerli bir ad giriniz.") String firstName,
            @NotBlank @Size(max = 50) @Pattern(regexp = NAME_PATTERN, message = "Geçerli bir soyad giriniz.") String lastName,
            @NotBlank @Tckn String tckn,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE) String password) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE) String newPassword) {
    }

    public record CsrfResponse(String headerName, String token) {
    }

    /** Oturumdaki kullanıcının kendisi; TCKN ve şifre asla dönmez. */
    public record UserResponse(Long id, String firstName, String lastName, String email) {
        public static UserResponse of(User u) {
            return new UserResponse(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail());
        }
    }
}
