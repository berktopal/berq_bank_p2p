package p2p_transfer.transfer;

import org.springframework.stereotype.Component;
import p2p_transfer.config.AppProperties;
import p2p_transfer.transfer.TransferDtos.Direction;
import p2p_transfer.transfer.TransferDtos.TransactionResponse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel (TR) ile doğrudan açılabilen CSV: UTF-8 BOM, ';' ayırıcı, ',' ondalık.
 * Metin hücreleri formül enjeksiyonuna (CSV injection) karşı kaçışlanır.
 */
@Component
public class TransactionCsvWriter {

    private static final String BOM = "﻿";
    private static final String HEADER = "Tarih;Referans;Yön;Karşı Taraf;Karşı IBAN;Hesap;Açıklama;Kategori;Tutar;Para Birimi;İşlem Sonrası Bakiye";

    private final DateTimeFormatter dateFormat;

    public TransactionCsvWriter(AppProperties props) {
        this.dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(props.bank().zone());
    }

    public byte[] write(List<TransactionResponse> rows) {
        StringBuilder sb = new StringBuilder(BOM).append(HEADER).append("\r\n");
        for (TransactionResponse t : rows) {
            boolean out = t.direction() == Direction.OUTGOING;
            sb.append(dateFormat.format(t.createdAt())).append(';')
                    .append(text(t.reference())).append(';')
                    .append(out ? "Giden" : "Gelen").append(';')
                    .append(text(t.counterparty().name())).append(';')
                    .append(text(t.counterparty().iban())).append(';')
                    .append(text(t.account().name())).append(';')
                    .append(text(t.description())).append(';')
                    .append(t.category()).append(';')
                    .append(number(out ? t.amount().negate() : t.amount())).append(';')
                    .append(t.currency()).append(';')
                    .append(number(t.balanceAfter())).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String text(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(";") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private static String number(BigDecimal value) {
        return value == null ? "" : value.toPlainString().replace('.', ',');
    }
}
