package p2p_transfer.transfer;

import org.junit.jupiter.api.Test;
import p2p_transfer.config.AppProperties;
import p2p_transfer.transfer.TransferDtos.AccountRef;
import p2p_transfer.transfer.TransferDtos.Counterparty;
import p2p_transfer.transfer.TransferDtos.Direction;
import p2p_transfer.transfer.TransferDtos.TransactionResponse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionCsvWriterTest {

    private final TransactionCsvWriter writer = new TransactionCsvWriter(new AppProperties(
            new AppProperties.Bank("00999", ZoneId.of("Europe/Istanbul"), 5),
            new AppProperties.Transfer(new BigDecimal("50000")),
            new AppProperties.Security(5, Duration.ofMinutes(15)),
            new AppProperties.Onboarding(BigDecimal.ZERO)));

    @Test
    void writesExcelFriendlyTurkishCsv() {
        String csv = new String(writer.write(List.of(row("Kira; Ekim", Direction.OUTGOING))), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("﻿Tarih;Referans;");
        // 2026-01-15T07:30Z = İstanbul 10:30; giden tutar negatif ve virgüllü
        assertThat(csv).contains("15.01.2026 10:30;BQ123;Giden;Ayşe Kaya;TR00;Ana;\"Kira; Ekim\";RENT;-1500,50;TRY;8499,50");
    }

    @Test
    void neutralisesSpreadsheetFormulas() {
        assertThat(TransactionCsvWriter.text("=HYPERLINK(\"http://evil\")")).isEqualTo("\"'=HYPERLINK(\"\"http://evil\"\")\"");
        assertThat(TransactionCsvWriter.text("+90 555")).isEqualTo("'+90 555");
        assertThat(TransactionCsvWriter.text("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(TransactionCsvWriter.text("Normal")).isEqualTo("Normal");
    }

    private static TransactionResponse row(String description, Direction direction) {
        return new TransactionResponse(1L, "BQ123", direction, false, new BigDecimal("1500.50"), "TRY", description,
                TransactionCategory.RENT, TransactionStatus.SUCCESS, Instant.parse("2026-01-15T07:30:00Z"),
                new AccountRef(1L, "TR11", "Ana"), new Counterparty("Ayşe Kaya", "TR00"), new BigDecimal("8499.50"));
    }
}
