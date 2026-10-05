package p2p_transfer.common.error;

import org.springframework.http.HttpStatus;

/**
 * API'nin döndürdüğü makine-okunur hata kodları.
 * Frontend bu kodları kendi dil dosyasında çevirir; {@code defaultMessage} yalnızca yedek metindir.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "İstek doğrulanamadı."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "İstek gövdesi okunamadı."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Bu işlem için giriş yapmanız gerekiyor."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Kayıt bulunamadı."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata oluştu."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Çok fazla istek gönderdiniz. Lütfen biraz bekleyin."),

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "E-posta veya şifre hatalı."),
    USER_LOCKED(HttpStatus.LOCKED, "Çok fazla hatalı deneme. Hesabınız geçici olarak kilitlendi."),
    EMAIL_TAKEN(HttpStatus.CONFLICT, "Bu e-posta adresi zaten kayıtlı."),
    TCKN_TAKEN(HttpStatus.CONFLICT, "Bu T.C. kimlik numarası zaten kayıtlı."),
    WRONG_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "Mevcut şifreniz hatalı."),

    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Hesap bulunamadı."),
    IBAN_NOT_FOUND(HttpStatus.NOT_FOUND, "Bu IBAN'a ait bir hesap bulunamadı."),
    ACCOUNT_LIMIT_REACHED(HttpStatus.UNPROCESSABLE_CONTENT, "Açabileceğiniz en fazla hesap sayısına ulaştınız."),

    SAME_ACCOUNT(HttpStatus.UNPROCESSABLE_CONTENT, "Aynı hesaba transfer yapamazsınız."),
    INSUFFICIENT_FUNDS(HttpStatus.UNPROCESSABLE_CONTENT, "Yetersiz bakiye."),
    CURRENCY_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT, "Farklı para birimindeki hesaplar arasında transfer yapılamaz."),
    DAILY_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_CONTENT, "Günlük transfer limitiniz aşılıyor."),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "İşlem bulunamadı."),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "Bu işlem anahtarı farklı bir transfer için kullanılmış."),

    CONTACT_NOT_FOUND(HttpStatus.NOT_FOUND, "Kayıtlı alıcı bulunamadı."),
    CONTACT_EXISTS(HttpStatus.CONFLICT, "Bu alıcı zaten kayıtlı."),
    CONTACT_IS_SELF(HttpStatus.UNPROCESSABLE_CONTENT, "Kendi hesabınızı alıcı olarak kaydedemezsiniz."),

    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Para isteği bulunamadı."),
    REQUEST_NOT_PENDING(HttpStatus.CONFLICT, "Bu para isteği artık beklemede değil."),
    REQUEST_SELF(HttpStatus.UNPROCESSABLE_CONTENT, "Kendinizden para isteyemezsiniz."),

    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "Talimat bulunamadı."),
    SCHEDULE_INVALID_DATES(HttpStatus.UNPROCESSABLE_CONTENT, "Talimat tarihleri geçersiz."),
    SCHEDULE_NOT_ACTIVE(HttpStatus.CONFLICT, "Talimat artık düzenlenemez."),

    BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "Bütçe bulunamadı."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Bildirim bulunamadı.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
