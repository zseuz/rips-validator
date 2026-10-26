package com.zseuz.rips.ia;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

/**
 * Elige el proveedor de IA. Con {@code rips.ia.proveedor=auto} (por defecto) usa Gemini si hay
 * GEMINI_API_KEY y, si no, Claude si hay ANTHROPIC_API_KEY. Sin ninguna clave la aplicación
 * arranca igual y la explicación responde 503 diciendo qué falta.
 */
@Configuration
class IaConfig {

    private static final Logger log = LoggerFactory.getLogger(IaConfig.class);

    /** El modelo elegido, o ninguno. Spring llama a close() al apagar (cierra el cliente de Claude). */
    record ProveedorIa(Optional<ModeloIa> modelo) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            if (modelo.isPresent() && modelo.get() instanceof AutoCloseable cerrable) {
                cerrable.close();
            }
        }
    }

    @Bean
    ProveedorIa proveedorIa(
            @Value("${rips.ia.proveedor}") String proveedor,
            @Value("${rips.ia.gemini.api-key:}") String claveGemini,
            @Value("${rips.ia.gemini.modelo}") String modeloGemini,
            @Value("${rips.ia.claude.api-key:}") String claveClaude,
            @Value("${rips.ia.claude.modelo}") String modeloClaude,
            JsonMapper json) {
        boolean gemini = !claveGemini.isBlank() && (proveedor.equals("gemini") || proveedor.equals("auto"));
        boolean claude = !claveClaude.isBlank() && (proveedor.equals("claude") || (proveedor.equals("auto") && !gemini));
        Optional<ModeloIa> modelo = gemini ? Optional.of(new GeminiIa(claveGemini, modeloGemini, json))
                : claude ? Optional.of(new ClaudeIa(claveClaude, modeloClaude))
                : Optional.empty();
        log.info("Explicación con IA: {}", modelo.map(ModeloIa::nombre).orElse("desactivada (sin clave)"));
        return new ProveedorIa(modelo);
    }
}
