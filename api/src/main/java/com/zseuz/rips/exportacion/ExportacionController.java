package com.zseuz.rips.exportacion;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.zseuz.rips.reglas.CodigoRegla;
import com.zseuz.rips.referencia.CodigoReferencia;
import com.zseuz.rips.referencia.TablasReferencia;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Datos del historial en CSV para Power BI (o Excel) y el proyecto de Power BI listo para abrir.
 * Los CSV se leen directo con SQL: son tablas planas y pueden tener muchas filas.
 */
@RestController
@RequestMapping("/api/v1/exportaciones")
class ExportacionController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("America/Bogota"));
    private static final Map<String, String> ESTADOS = Map.of(
            "VALIDO", "Válido",
            "VALIDO_CON_ADVERTENCIAS", "Con advertencias",
            "RECHAZADO", "Rechazado");

    private final JdbcTemplate jdbc;
    private final TablasReferencia tablas;
    private final ReportePowerBi reporte;

    ExportacionController(JdbcTemplate jdbc, TablasReferencia tablas, ReportePowerBi reporte) {
        this.jdbc = jdbc;
        this.tablas = tablas;
        this.reporte = reporte;
    }

    @Operation(summary = "Una fila por validación (CSV)")
    @GetMapping("/validaciones.csv")
    ResponseEntity<StreamingResponseBody> validaciones() {
        return csv("validaciones.csv", salida -> {
            salida.fila("IdValidacion", "Factura", "Obligado", "Estado", "FechaValidacion", "Usuarios", "Servicios", "Errores", "Advertencias");
            jdbc.query("select id, num_factura, num_documento_id_obligado, estado, fecha_validacion, total_usuarios, "
                    + "total_servicios, total_errores, total_advertencias from validacion order by id", fila -> {
                        Timestamp fecha = fila.getTimestamp("fecha_validacion");
                        salida.fila(fila.getLong("id"), fila.getString("num_factura"), fila.getString("num_documento_id_obligado"),
                                ESTADOS.getOrDefault(fila.getString("estado"), fila.getString("estado")),
                                fecha == null ? null : FECHA.format(fecha.toInstant()),
                                fila.getInt("total_usuarios"), fila.getInt("total_servicios"),
                                fila.getLong("total_errores"), fila.getLong("total_advertencias"));
                    });
        });
    }

    @Operation(summary = "Una fila por hallazgo, con el nombre oficial del código cuando es CIE-10, CUPS, municipio o país (CSV)")
    @GetMapping("/hallazgos.csv")
    ResponseEntity<StreamingResponseBody> hallazgos() {
        return csv("hallazgos.csv", salida -> {
            salida.fila("IdValidacion", "Orden", "Regla", "Severidad", "Usuario", "TipoServicio", "Campo", "Ruta", "Mensaje", "Valor",
                    "TablaReferencia", "NombreReferencia");
            jdbc.query("select validacion_id, orden, codigo, severidad, ruta, mensaje, valor from hallazgo order by validacion_id, orden", fila -> {
                String ruta = fila.getString("ruta");
                String valor = fila.getString("valor");
                UbicacionHallazgo ubicacion = UbicacionHallazgo.de(ruta);
                // Solo hay código que nombrar si el campo vino diligenciado (un RV001 no tiene valor).
                String tabla = valor == null ? null : ubicacion.tablaReferencia();
                String nombre = tabla == null ? null
                        : tablas.buscar(tabla, valor).map(CodigoReferencia::nombre).orElse("(no existe en SISPRO)");
                salida.fila(fila.getLong("validacion_id"), fila.getInt("orden") + 1, fila.getString("codigo"), fila.getString("severidad"),
                        ubicacion.usuario(), ubicacion.tipoServicio(), ubicacion.campo(), ruta, fila.getString("mensaje"), valor,
                        tabla, nombre);
            });
        });
    }

    @Operation(summary = "Catálogo de reglas con su grupo (CSV)")
    @GetMapping("/reglas.csv")
    ResponseEntity<StreamingResponseBody> reglas() {
        return csv("reglas.csv", salida -> {
            salida.fila("Regla", "Severidad", "Titulo", "Grupo");
            Arrays.stream(CodigoRegla.values()).forEach(r -> salida.fila(r.name(), r.severidad(), r.titulo(), grupo(r)));
        });
    }

    @Operation(summary = "Proyecto de Power BI (PBIP) en .zip, apuntando a esta API",
            description = "Se abre RipsValidator.pbip en Power BI Desktop y se pulsa Actualizar con la API encendida.")
    @GetMapping("/power-bi.zip")
    ResponseEntity<StreamingResponseBody> powerBi() {
        String urlApi = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"RipsValidator-PowerBI.zip\"")
                .body(salida -> reporte.escribirZip(urlApi, salida));
    }

    /** Agrupa las reglas como en el catálogo: la decena del código dice de qué trata. */
    static String grupo(CodigoRegla regla) {
        return switch (regla.name().charAt(3)) {
            case '0' -> "Estructura y tablas de SISPRO";
            case '1' -> "Transacción y usuarios";
            case '2' -> "Fechas y edad";
            case '3' -> "Coherencia clínica";
            case '4' -> "Valores";
            default -> "Factura electrónica";
        };
    }

    private static ResponseEntity<StreamingResponseBody> csv(String nombre, Consumer<Csv> contenido) {
        StreamingResponseBody cuerpo = flujo -> {
            Writer escritor = new OutputStreamWriter(flujo, StandardCharsets.UTF_8);
            contenido.accept(new Csv(escritor));
            escritor.flush();
        };
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .body(cuerpo);
    }
}
