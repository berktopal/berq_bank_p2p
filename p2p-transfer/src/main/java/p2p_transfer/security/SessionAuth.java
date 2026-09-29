package p2p_transfer.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Basit, sunucu taraflı oturum yönetimi.
 * Giriş başarılı olunca kullanıcı ID'si HttpSession'a yazılır; tarayıcı yalnızca
 * HttpOnly + SameSite=Strict bir oturum çerezi tutar. Kimlik bilgisi asla
 * istemciden gelen bir alana (ör. senderAccountId) güvenilerek belirlenmez.
 */
public final class SessionAuth {

    public static final String USER_ID = "AUTH_USER_ID";

    private SessionAuth() {
    }

    public static Long requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object id = session == null ? null : session.getAttribute(USER_ID);
        if (id instanceof Long userId) {
            return userId;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bu işlem için giriş yapmanız gerekiyor.");
    }

    /** Session fixation'a karşı: eski oturumu kapatıp yenisini açar. */
    public static void signIn(HttpServletRequest request, Long userId) {
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        request.getSession(true).setAttribute(USER_ID, userId);
    }

    public static void signOut(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
