package p2p_transfer.support;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Test süreci boyunca tek bir gerçek PostgreSQL 16 sunucusu. Docker gerektirmez:
 * binary'ler Maven bağımlılığı olarak gelir, ilk kullanımda başlatılır ve JVM kapanınca durdurulur.
 */
public final class EmbeddedPostgres {

    private static io.zonky.test.db.postgres.embedded.EmbeddedPostgres instance;

    private EmbeddedPostgres() {
    }

    public static synchronized io.zonky.test.db.postgres.embedded.EmbeddedPostgres get() {
        if (instance == null) {
            try {
                instance = io.zonky.test.db.postgres.embedded.EmbeddedPostgres.builder()
                        // İşletim sistemi locale'inden bağımsız: initdb, "Turkish_Türkiye.1254" gibi ASCII dışı adları reddeder
                        .setLocaleConfig("locale", "C")
                        .setServerConfig("timezone", "UTC")
                        .setServerConfig("max_connections", "200")
                        .start();
            } catch (IOException e) {
                throw new UncheckedIOException("Embedded PostgreSQL could not be started", e);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    instance.close();
                } catch (IOException ignored) {
                    // süreç zaten kapanıyor
                }
            }));
        }
        return instance;
    }

    public static String jdbcUrl(String database) {
        return get().getJdbcUrl("postgres", database);
    }
}
