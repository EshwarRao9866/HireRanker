package spring.eshwar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Value("${application.ai.openrouter.timeout-seconds:${application.ai.timeout-seconds:25}}")
    private int timeoutSeconds;

    @Bean
    public RestClient aiRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 10)));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
