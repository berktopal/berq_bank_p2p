package p2p_transfer.controller;

import org.springframework.web.bind.annotation.*;
import p2p_transfer.entity.Transaction;
import p2p_transfer.service.TransactionService;

import java.math.BigDecimal;
import java.util.List; // Listeleme için bu import şart!
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // --- YENİ EKLENEN LİSTELEME KAPISI (GET) ---
    @GetMapping
    public List<Transaction> getAll() {
        return transactionService.getAllTransactions();
    }

    @PostMapping("/transfer")
    public Transaction transfer(@RequestBody Map<String, Object> request) {
        // Postman'den gelen JSON verilerini parçalıyoruz
        Long senderId = Long.valueOf(request.get("senderAccountId").toString());
        Long receiverId = Long.valueOf(request.get("receiverAccountId").toString());
        BigDecimal amount = new BigDecimal(request.get("amount").toString());

        return transactionService.transferMoney(senderId, receiverId, amount);
    }
}