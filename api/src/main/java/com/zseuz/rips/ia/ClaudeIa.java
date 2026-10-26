package com.zseuz.rips.ia;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
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

/** Claude con el SDK oficial de Anthropic; el SDK deriva el esquema JSON del record {@link Explicacion}. */
final class ClaudeIa implements ModeloIa, AutoCloseable {

    private final AnthropicClient cliente;
    private final String modelo;

    ClaudeIa(String clave, String modelo) {
        this.cliente = AnthropicOkHttpClient.builder().apiKey(clave).build();
        this.modelo = modelo;
    }

    @Override
    public String nombre() {
        return modelo;
    }

    @Override
    public Explicacion explicar(String instrucciones, String hallazgo) {
        StructuredMessageCreateParams<Explicacion> params = MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(8000L)
                .system(instrucciones)
                .addUserMessage(hallazgo)
                // Tarea corta y acotada: esfuerzo bajo basta y responde más rápido.
                .outputConfig(Explicacion.class, BetaOutputConfig.Effort.LOW)
                // Si el modelo declina, la API reintenta con otro modelo según la categoría del rechazo.
                .addBeta("server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .build();
        try {
            var respuesta = cliente.beta().messages().create(params);
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

    @Override
    public void close() {
        cliente.close();
    }
}
