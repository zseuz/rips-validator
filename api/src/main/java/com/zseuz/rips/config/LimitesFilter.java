package com.zseuz.rips.config;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Protege la validación, que es la operación cara: rechaza archivos más grandes que el límite
 * (413) y más de N validaciones por minuto desde la misma IP (429). El conteo es en memoria y por
 * instancia: suficiente para una sola API; con varias réplicas iría en Redis o en el proxy.
 */
@Component
class LimitesFilter extends OncePerRequestFilter {

    private static final String RUTA_VALIDAR = "/api/v1/validaciones";

    private final long tamanoMaximo;
    private final int porMinuto;
    private final Clock reloj;
    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();

    LimitesFilter(@Value("${rips.limites.tamano-maximo}") DataSize tamanoMaximo,
            @Value("${rips.limites.validaciones-por-minuto}") int porMinuto, Clock reloj) {
        this.tamanoMaximo = tamanoMaximo.toBytes();
        this.porMinuto = porMinuto;
        this.reloj = reloj;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // POST /api/v1/validaciones y /api/v1/validaciones/con-factura
        return !("POST".equals(request.getMethod()) && request.getRequestURI().startsWith(RUTA_VALIDAR));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > tamanoMaximo) {
            rechazar(response, HttpStatus.CONTENT_TOO_LARGE,
                    "El archivo supera el máximo de " + DataSize.ofBytes(tamanoMaximo).toMegabytes() + " MB");
            return;
        }
        long minuto = reloj.millis() / 60_000;
        Ventana ventana = ventanas.compute(request.getRemoteAddr(),
                (ip, actual) -> actual == null || actual.minuto() != minuto ? new Ventana(minuto, 1) : actual.sumar());
        if (ventana.peticiones() > porMinuto) {
            response.setHeader("Retry-After", String.valueOf(60 - (reloj.millis() / 1000) % 60));
            rechazar(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Máximo " + porMinuto + " validaciones por minuto; intente de nuevo en unos segundos");
            return;
        }
        if (ventanas.size() > 10_000) {
            ventanas.values().removeIf(v -> v.minuto() != minuto); // no acumular IPs viejas
        }
        cadena.doFilter(request, response);
    }

    private static void rechazar(HttpServletResponse response, HttpStatus estado, String detalle) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + estado.value() + ",\"title\":\"" + estado.getReasonPhrase()
                + "\",\"detail\":\"" + detalle + "\"}");
    }

    private record Ventana(long minuto, int peticiones) {
        Ventana sumar() {
            return new Ventana(minuto, peticiones + 1);
        }
    }
}
