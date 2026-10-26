package com.zseuz.rips.ia;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.anthropic.models.beta.messages.StructuredMessageCreateParams;
import com.zseuz.rips.reglas.CodigoRegla;
import com.zseuz.rips.reglas.Hallazgo;

/**
 * Explica un hallazgo en lenguaje sencillo con Claude. Las explicaciones se guardan en memoria por
 * validación y hallazgo: pedir dos veces la misma no vuelve a llamar (ni a cobrar) la API.
 */
@Service
public class ExplicadorHallazgos {

    private static final String SISTEMA = """
            Eres un auditor de cuentas médicas en Colombia experto en RIPS (Resolución 2275 de 2023), factura \
            electrónica de venta en salud y las tablas de referencia de SISPRO. Un validador automático encontró un \
            hallazgo en un archivo RIPS en JSON. Explícaselo a la persona de facturación de una IPS que no es \
            programadora: qué significa, cómo corregirlo y qué pasa si no lo corrige. Sé concreto y breve (máximo \
            tres frases por campo), en español de Colombia. Si el hallazgo es una advertencia, dilo: no bloquea el \
            envío pero puede terminar en glosa. No inventes normas ni números de artículo.""";

    /** Campos con datos personales: su valor no se envía a la API. */
    private static final Set<String> CAMPOS_PERSONALES = Set.of("numDocumentoIdentificacion", "fechaNacimiento", "usuarios");

    private final ObjectProvider<AnthropicClient> cliente;
    private final String modelo;
    private final Map<String, Explicacion> cache = new ConcurrentHashMap<>();

    ExplicadorHallazgos(ObjectProvider<AnthropicClient> cliente, @Value("${rips.ia.modelo}") String modelo) {
        this.cliente = cliente;
        this.modelo = modelo;
    }

    public boolean disponible() {
        return cliente.getIfAvailable() != null;
    }

    public String modelo() {
        return modelo;
    }

    public Explicacion explicar(long validacion, int orden, Hallazgo hallazgo) {
        AnthropicClient api = cliente.getIfAvailable();
        if (api == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La explicación con IA no está configurada: falta la variable ANTHROPIC_API_KEY en la API");
        }
        return cache.computeIfAbsent(validacion + "/" + orden, clave -> llamar(api, hallazgo));
    }

    private Explicacion llamar(AnthropicClient api, Hallazgo hallazgo) {
        StructuredMessageCreateParams<Explicacion> params = MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(8000L)
                .system(SISTEMA)
                .addUserMessage(describir(hallazgo))
                // Tarea corta y acotada: esfuerzo bajo basta y responde más rápido.
                .outputConfig(Explicacion.class, BetaOutputConfig.Effort.LOW)
                // Si el modelo declina, la API reintenta con otro modelo según la categoría del rechazo.
                .addBeta("server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .build();
        try {
            var respuesta = api.beta().messages().create(params);
            if (respuesta.stopReason().filter(BetaStopReason.REFUSAL::equals).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El modelo no pudo explicar este hallazgo");
            }
            return respuesta.content().stream()
                    .flatMap(bloque -> bloque.text().stream())
                    .map(texto -> texto.text())
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El modelo no devolvió una explicación"));
        } catch (RateLimitException e) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "La API de Claude está saturada; intenta en un momento");
        } catch (UnauthorizedException | PermissionDeniedException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "La clave ANTHROPIC_API_KEY no es válida");
        } catch (AnthropicServiceException | AnthropicIoException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo consultar la API de Claude");
        }
    }

    /** El hallazgo como texto para el modelo, sin datos personales del usuario. */
    static String describir(Hallazgo hallazgo) {
        String campo = hallazgo.ruta().substring(hallazgo.ruta().lastIndexOf('.') + 1).replaceAll("\\[\\d+]$", "");
        String titulo = Optional.of(hallazgo.codigo()).map(CodigoRegla::valueOf).map(CodigoRegla::titulo).orElse("");
        String valor = hallazgo.valor() == null ? "(vacío)"
                : CAMPOS_PERSONALES.contains(campo) ? "(omitido: dato personal)" : hallazgo.valor();
        return """
                Regla: %s – %s
                Severidad: %s
                Campo: %s (ruta en el JSON: %s)
                Valor recibido: %s
                Mensaje del validador: %s""".formatted(hallazgo.codigo(), titulo, hallazgo.severidad(), campo,
                hallazgo.ruta(), valor, hallazgo.mensaje());
    }
}
