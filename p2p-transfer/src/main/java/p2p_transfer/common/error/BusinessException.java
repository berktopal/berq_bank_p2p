package p2p_transfer.common.error;

import java.util.Map;

/**
 * İş kuralı ihlalleri için tek exception tipi. HTTP durumunu {@link ErrorCode} belirler;
 * {@code details} istemciye ek bağlam (ör. kalan limit) taşır.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final transient Map<String, Object> details;

    public BusinessException(ErrorCode code) {
        this(code, code.defaultMessage(), Map.of());
    }

    public BusinessException(ErrorCode code, Map<String, Object> details) {
        this(code, code.defaultMessage(), details);
    }

    public BusinessException(ErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public ErrorCode code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
