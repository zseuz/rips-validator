package com.zseuz.rips.ia;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gemini por su API REST (generateContent). La respuesta se pide en JSON con un esquema, así que
 * llega con los mismos campos que {@link Explicacion}. La clave va en un encabezado, nunca en la URL.
 */
final class GeminiIa implements ModeloIa {

    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent";

    private static final Map<String, Object> ESQUEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "queSignifica", campo("Qué significa el hallazgo, en una o dos frases sencillas"),
                    "comoCorregirlo", campo("Pasos concretos para corregirlo en el archivo o en el sistema que genera el RIPS"),
                    "impacto", campo("Qué pasa si no se corrige: rechazo del Ministerio, glosa de la EPS o riesgo de auditoría")),
            "required", List.of("queSignifica", "comoCorregirlo", "impacto"),
            "propertyOrdering", List.of("queSignifica", "comoCorregirlo", "impacto"));

    private final RestClient http;
    private final JsonMapper json;
    private final String modelo;

    /** Reintentos cuando Gemini responde 500/503 (sobrecarga pasajera), con espera creciente. */
    private static final int REINTENTOS = 2;

    GeminiIa(String clave, String modelo, JsonMapper json) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(60));
        this.http = RestClient.builder().requestFactory(fabrica).defaultHeader("x-goog-api-key", clave).build();
        this.json = json;
        this.modelo = modelo;
    }

    @Override
    public String nombre() {
        return modelo;
    }

    @Override
    public Explicacion explicar(String instrucciones, String hallazgo) {
        Map<String, Object> cuerpo = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", instrucciones))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", hallazgo)))),
                "generationConfig", Map.of("responseMimeType", "application/json", "responseSchema", ESQUEMA));
        try {
            JsonNode respuesta = pedir(cuerpo, REINTENTOS);
            JsonNode candidato = respuesta == null ? null : respuesta.path("candidates").path(0);
            String texto = candidato == null ? "" : candidato.path("content").path("parts").path(0).path("text").asString("");
            if (texto.isBlank()) {
                String motivo = candidato == null ? "" : candidato.path("finishReason").asString("");
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Gemini no devolvió una explicación" + (motivo.isBlank() ? "" : " (" + motivo + ")"));
            }
            return json.readValue(texto, Explicacion.class);
        } catch (RestClientResponseException e) {
            HttpStatus estado = switch (e.getStatusCode().value()) {
                case 429 -> HttpStatus.TOO_MANY_REQUESTS;
                case 401, 403 -> HttpStatus.SERVICE_UNAVAILABLE;
                default -> HttpStatus.BAD_GATEWAY;
            };
            String mensaje = switch (e.getStatusCode().value()) {
                case 429 -> "Se alcanzó el límite de la API de Gemini; intenta en un momento";
                case 500, 503 -> "Gemini está sobrecargado en este momento; intenta de nuevo en unos minutos";
                case 401, 403 -> "La clave GEMINI_API_KEY no es válida o no tiene acceso al modelo " + modelo;
                case 404 -> "El modelo " + modelo + " no existe en la API de Gemini (revisa rips.ia.gemini.modelo)";
                default -> "La API de Gemini respondió con un error (" + e.getStatusCode().value() + ")";
            };
            throw new ResponseStatusException(estado, mensaje);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo conectar con la API de Gemini");
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini devolvió una explicación con un formato inesperado");
        }
    }

    private JsonNode pedir(Map<String, Object> cuerpo, int reintentos) {
        try {
            return http.post().uri(URL, modelo).contentType(MediaType.APPLICATION_JSON).body(cuerpo).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException e) {
            int codigo = e.getStatusCode().value();
            if ((codigo == 500 || codigo == 503) && reintentos > 0) {
                esperar(Duration.ofSeconds(2L * (REINTENTOS - reintentos + 1)));
                return pedir(cuerpo, reintentos - 1);
            }
            throw e;
        }
    }

    private static void esperar(Duration tiempo) {
        try {
            Thread.sleep(tiempo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Map<String, String> campo(String descripcion) {
        return Map.of("type", "STRING", "description", descripcion);
    }
}
