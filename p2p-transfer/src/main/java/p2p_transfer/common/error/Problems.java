package p2p_transfer.common.error;

import org.springframework.http.ProblemDetail;

import java.net.URI;

public final class Problems {

    private Problems() {
    }

    public static ProblemDetail of(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setType(URI.create("https://berqbank.dev/errors/" + code.name().toLowerCase().replace('_', '-')));
        problem.setTitle(code.status().getReasonPhrase());
        problem.setProperty("code", code.name());
        return problem;
    }
}
