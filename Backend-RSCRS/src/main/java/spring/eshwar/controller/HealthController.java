package spring.eshwar.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:4202", "http://127.0.0.1:4200", "http://127.0.0.1:4201"}, allowCredentials = "true")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth(HttpServletRequest request) {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "HireRanker Spring Boot Backend");
        status.put("port", request != null ? request.getServerPort() : 8080);
        status.put("timestamp", LocalDateTime.now());
        status.put("version", "1.0.0");

        boolean dbUp = false;
        try (Connection conn = dataSource.getConnection()) {
            dbUp = conn.isValid(2);
        } catch (Exception ex) {
            log.warn("Database health check probe failed: {}", ex.getMessage());
            dbUp = false;
        }

        if (dbUp) {
            status.put("status", "UP");
            status.put("backend", "CONNECTED");
            status.put("database", "UP");
            return ResponseEntity.ok(status);
        } else {
            status.put("status", "DEGRADED");
            status.put("backend", "CONNECTED");
            status.put("database", "DOWN");
            status.put("message", "Backend is running but MySQL database is unreachable.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(status);
        }
    }
}
