package spring.eshwar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import org.springframework.scheduling.annotation.EnableAsync;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
@EnableAsync
public class HireRanker {
    private static final Logger logger = LoggerFactory.getLogger(HireRanker.class);

    private final Environment env;
    private final DataSource dataSource;

    public HireRanker(Environment env, DataSource dataSource) {
        this.env = env;
        this.dataSource = dataSource;
    }

    public static void main(String[] args) {
        SpringApplication.run(HireRanker.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        String port = env.getProperty("server.port", "8080");
        String appName = env.getProperty("spring.application.name", "hireranker-backend");

        boolean dbConnected = false;
        try (Connection conn = dataSource.getConnection()) {
            dbConnected = conn.isValid(2);
        } catch (Exception ignored) {
            dbConnected = false;
        }

        logger.info("\n" +
                "====================================================================\n" +
                "  HireRanker Backend Started Successfully!\n" +
                "  Application : {}\n" +
                "  Port        : {}\n" +
                "  Database    : {}\n" +
                "  Health URL  : http://localhost:{}/api/health\n" +
                "  Status      : {}\n" +
                "====================================================================",
                appName,
                port,
                dbConnected ? "CONNECTED (MySQL)" : "UNREACHABLE",
                port,
                dbConnected ? "ONLINE" : "DEGRADED (MySQL Offline)"
        );
    }
}
