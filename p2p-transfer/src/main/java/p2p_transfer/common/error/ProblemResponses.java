package p2p_transfer.common.error;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servlet filtrelerinde (@RestControllerAdvice'ın devreye girmediği yerlerde) Problem Details yanıtı yazar.
 * Gövde sabit metinlerden ve sayısal ek alanlardan oluşur; kullanıcı girdisi içermediği için elle JSON üretmek güvenlidir.
 */
public final class ProblemResponses {

    private ProblemResponses() {
    }

    public static void write(HttpServletResponse response, ErrorCode code) throws IOException {
        write(response, code, Map.of());
    }

    public static void write(HttpServletResponse response, ErrorCode code, Map<String, Number> extra) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String extras = extra.entrySet().stream()
                .map(e -> ",\"%s\":%s".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining());
        response.getWriter().write("""
                {"type":"https://berqbank.dev/errors/%s","title":"%s","status":%d,"detail":"%s","code":"%s"%s}"""
                .formatted(code.name().toLowerCase().replace('_', '-'), code.status().getReasonPhrase(),
                        code.status().value(), code.defaultMessage(), code.name(), extras));
    }
}
