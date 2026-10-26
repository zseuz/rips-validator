package com.zseuz.rips.referencia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tablas de referencia de SISPRO en memoria, leídas de {@code catalogos/*.csv} dentro del jar.
 * Las reglas consultan aquí (miles de búsquedas por archivo); la copia en base de datos es para
 * consultas y reportes.
 */
@Component
public class TablasReferencia {

    private static final String UBICACION = "classpath*:catalogos/";

    private final Map<String, Map<String, CodigoReferencia>> tablas = new TreeMap<>();
    private final Map<String, Instant> descargadas = new HashMap<>();

    public TablasReferencia() {
        try {
            PathMatchingResourcePatternResolver recursos = new PathMatchingResourcePatternResolver();
            for (Resource csv : recursos.getResources(UBICACION + "*.csv")) {
                String tabla = csv.getFilename().replace(".csv", "");
                tablas.put(tabla, leerTabla(tabla, csv));
            }
            for (Resource meta : recursos.getResources(UBICACION + "metadatos.json")) {
                JsonNode porTabla = JsonMapper.builder().build().readTree(meta.getContentAsString(StandardCharsets.UTF_8)).path("tablas");
                for (String tabla : porTabla.propertyNames()) {
                    String fecha = porTabla.path(tabla).path("descargada").asString(null);
                    if (fecha != null) {
                        descargadas.put(tabla, OffsetDateTime.parse(fecha).toInstant());
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudieron leer las tablas de referencia", e);
        }
    }

    /** Busca un código. Una tabla que no se descargó se comporta como vacía; use {@link #tiene}. */
    public Optional<CodigoReferencia> buscar(String tabla, String codigo) {
        return Optional.ofNullable(tablas.getOrDefault(tabla, Map.of()).get(codigo));
    }

    public boolean tiene(String tabla) {
        return tablas.containsKey(tabla);
    }

    public List<ResumenTabla> resumen() {
        return tablas.entrySet().stream()
                .map(e -> new ResumenTabla(e.getKey(), e.getValue().size(), descargadas.get(e.getKey())))
                .toList();
    }

    Collection<Map<String, CodigoReferencia>> todas() {
        return tablas.values();
    }

    private static Map<String, CodigoReferencia> leerTabla(String tabla, Resource csv) throws IOException {
        Map<String, CodigoReferencia> codigos = new HashMap<>();
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {
            for (Map<String, String> fila : LectorCsv.leer(lector)) {
                CodigoReferencia codigo = new CodigoReferencia(
                        tabla,
                        fila.get("codigo"),
                        fila.get("nombre"),
                        "SI".equalsIgnoreCase(fila.get("habilitado")),
                        sexo(fila.get("AplicaASexo")),
                        entero(fila.get("EdadMinima")),
                        entero(fila.get("EdadMaxima")),
                        vacioANull(fila.get("UsoCodigoCUP")));
                codigos.put(codigo.codigo(), codigo);
            }
        }
        return codigos;
    }

    /** En la CIE-10 de SISPRO, AplicaASexo es 1 = hombre, 2 = mujer y 3 = ambos. */
    private static String sexo(String aplicaASexo) {
        return switch (aplicaASexo == null ? "" : aplicaASexo.trim()) {
            case "1" -> "H";
            case "2" -> "M";
            default -> null;
        };
    }

    private static Integer entero(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    public record ResumenTabla(String tabla, int registros, Instant descargada) {
    }
}
