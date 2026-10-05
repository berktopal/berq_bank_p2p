package p2p_transfer.transfer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import p2p_transfer.auth.AuthUser;
import p2p_transfer.common.PageResponse;
import p2p_transfer.transfer.TransferDtos.Direction;
import p2p_transfer.transfer.TransferDtos.TransactionFilter;
import p2p_transfer.transfer.TransferDtos.TransactionResponse;
import p2p_transfer.transfer.TransferDtos.TransferRequest;
import p2p_transfer.transfer.TransferService.TransferResult;

import java.time.LocalDate;

@Tag(name = "Transactions")
@Validated
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransferService transferService;
    private final TransactionQueryService queryService;
    private final TransactionCsvWriter csvWriter;

    public TransactionController(TransferService transferService, TransactionQueryService queryService,
                                 TransactionCsvWriter csvWriter) {
        this.transferService = transferService;
        this.queryService = queryService;
        this.csvWriter = csvWriter;
    }

    @Operation(summary = "İşlem geçmişi (filtreli, sayfalı, en yeni önce)")
    @GetMapping
    public PageResponse<TransactionResponse> search(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Direction direction,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return queryService.search(me.id(), new TransactionFilter(accountId, direction, from, to, category, q), page, size);
    }

    @Operation(summary = "Filtrelenmiş işlemleri CSV olarak indir (en fazla 5000 satır)")
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Direction direction,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionCategory category,
            @RequestParam(required = false) String q) {
        var rows = queryService.export(me.id(), new TransactionFilter(accountId, direction, from, to, category, q));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"berqbank-hareketler.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvWriter.write(rows));
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return queryService.get(me.id(), id, null);
    }

    @Operation(summary = "Para gönder",
            description = "Aynı Idempotency-Key ile tekrarlanan istek ikinci kez para göndermez; ilk sonucu döndürür.")
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @AuthenticationPrincipal AuthUser me,
            @RequestHeader(value = "Idempotency-Key", required = false)
            @Pattern(regexp = "^[A-Za-z0-9-]{8,64}$", message = "Geçersiz Idempotency-Key.") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        TransferResult result = transferService.transfer(me.id(), request, idempotencyKey);
        TransactionResponse body = queryService.get(me.id(), result.transaction().getId(), request.fromAccountId());
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header("Idempotent-Replayed", String.valueOf(result.replayed()))
                .body(body);
    }
}
