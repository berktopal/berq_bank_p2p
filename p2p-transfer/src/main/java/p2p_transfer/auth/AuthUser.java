package p2p_transfer.auth;

import java.io.Serial;
import java.io.Serializable;

/** Oturumda tutulan kimlik. Controller'lara {@code @AuthenticationPrincipal} ile enjekte edilir. */
public record AuthUser(Long id, String email) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
