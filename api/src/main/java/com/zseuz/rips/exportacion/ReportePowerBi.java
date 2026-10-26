package com.zseuz.rips.exportacion;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * El proyecto de Power BI de la carpeta powerbi/ (empaquetado en el jar), listo para entregar en
 * .zip. Lo único que cambia por descarga es el parámetro UrlApi del modelo, para que el reporte
 * lea los datos de la misma API desde la que se descargó.
 */
@Component
class ReportePowerBi {

    /** Valor del parámetro UrlApi en model.bim, tal como aparece escapado dentro del JSON. */
    static final String URL_POR_DEFECTO = "\\\"http://localhost:8080\\\" meta [IsParameterQuery=true";
    private static final Pattern URL_SEGURA = Pattern.compile("https?://[A-Za-z0-9.\\-]+(:\\d+)?(/[A-Za-z0-9._\\-/]*)?");
    private static final String CARPETA = "RipsValidator/";

    private final Map<String, byte[]> archivos = new TreeMap<>();

    ReportePowerBi() {
        try {
            for (Resource recurso : new PathMatchingResourcePatternResolver().getResources("classpath*:powerbi/**")) {
                String url = recurso.getURL().toString();
                if (recurso.isReadable() && !url.endsWith("/")) {
                    archivos.put(url.substring(url.lastIndexOf("/powerbi/") + "/powerbi/".length()), recurso.getContentAsByteArray());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el proyecto de Power BI", e);
        }
    }

    void escribirZip(String urlApi, OutputStream salida) throws IOException {
        if (!URL_SEGURA.matcher(urlApi).matches()) {
            throw new IllegalArgumentException("URL de la API no válida: " + urlApi);
        }
        ZipOutputStream zip = new ZipOutputStream(salida);
        for (Map.Entry<String, byte[]> archivo : archivos.entrySet()) {
            byte[] contenido = archivo.getValue();
            if (archivo.getKey().endsWith("model.bim")) {
                contenido = new String(contenido, StandardCharsets.UTF_8)
                        .replace(URL_POR_DEFECTO, URL_POR_DEFECTO.replace("http://localhost:8080", urlApi))
                        .getBytes(StandardCharsets.UTF_8);
            }
            zip.putNextEntry(new ZipEntry(CARPETA + archivo.getKey()));
            zip.write(contenido);
            zip.closeEntry();
        }
        zip.finish();
    }

    Map<String, byte[]> archivos() {
        return archivos;
    }
}
