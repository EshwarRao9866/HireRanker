package spring.eshwar.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:4200",
            "http://localhost:4201",
            "http://localhost:4202",
            "http://localhost:3000",
            "http://127.0.0.1:4200",
            "http://127.0.0.1:4201",
            "http://127.0.0.1:4202",
            "http://127.0.0.1:3000"
    );

    private static final List<String> ALLOWED_ORIGIN_PATTERNS = Arrays.asList(
            "http://localhost:[*]",
            "http://127.0.0.1:[*]",
            "http://localhost:*",
            "http://127.0.0.1:*"
    );

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(ALLOWED_ORIGINS.toArray(new String[0]))
                .allowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "Access-Control-Request-Method",
                        "Access-Control-Request-Headers",
                        "X-Requested-With"
                )
                .exposedHeaders(
                        "Authorization",
                        "Content-Disposition"
                )
                .allowCredentials(true)
                .maxAge(3600L);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowCredentials(true);
        config.setAllowedOrigins(ALLOWED_ORIGINS);
        config.setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-Requested-With"
        ));
        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
