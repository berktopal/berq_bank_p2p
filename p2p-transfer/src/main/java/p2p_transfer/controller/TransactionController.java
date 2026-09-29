package p2p_transfer.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import p2p_transfer.dto.ApiViews.TransactionView;
import p2p_transfer.security.SessionAuth;
import p2p_transfer.service.TransactionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Sadece giriş yapan kullanıcının işlemleri
    @GetMapping
    public List<TransactionView> getMine(HttpServletRequest request) {
        Long userId = SessionAuth.requireUserId(request);
        return transactionService.getTransactionsForUser(userId);
    }

    @PostMapping("/transfer")
    public TransactionView transfer(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = SessionAuth.requireUserId(request);
        Long senderId = parseLong(body.get("senderAccountId"), "senderAccountId");
        Long receiverId = parseLong(body.get("receiverAccountId"), "receiverAccountId");
        BigDecimal amount = parseAmount(body.get("amount"));

        return transactionService.transferMoney(userId, senderId, receiverId, amount);
    }

    private static Long parseLong(Object value, String field) {
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçersiz " + field + ".");
        }
    }

    private static BigDecimal parseAmount(Object value) {
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geçersiz tutar.");
        }
    }
}
