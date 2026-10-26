package com.zseuz.rips.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    private final String[] origenesPermitidos;

    AppConfig(@Value("${rips.cors.origenes-permitidos}") String[] origenesPermitidos) {
        this.origenesPermitidos = origenesPermitidos;
    }

    /**
     * Reloj en hora de Colombia. Las reglas de fechas ("no puede ser futura") lo usan en vez de
     * {@code LocalDate.now()} para que las pruebas fijen el día.
     */
    @Bean
    Clock reloj() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origenesPermitidos).allowedMethods("GET", "POST");
    }
}
