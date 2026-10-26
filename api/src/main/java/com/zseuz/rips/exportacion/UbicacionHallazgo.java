package com.zseuz.rips.exportacion;

import static java.util.Map.entry;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zseuz.rips.referencia.Tablas;

/**
 * Columnas de análisis que se sacan de la ruta de un hallazgo, para agrupar en Power BI.
 * {@code usuarios[1].servicios.consultas[0].codDiagnosticoPrincipal} → usuario 2, Consulta, codDiagnosticoPrincipal.
 *
 * @param usuario posición del usuario contando desde 1; {@code null} si el hallazgo es de la transacción
 * @param tablaReferencia tabla de SISPRO del campo cuando es un código que vale la pena nombrar (CIE-10, CUPS…)
 */
record UbicacionHallazgo(Integer usuario, String tipoServicio, String campo, String tablaReferencia) {

    private static final Pattern USUARIO = Pattern.compile("^usuarios\\[(\\d+)]");
    private static final Pattern SERVICIO = Pattern.compile("servicios\\.(consultas|procedimientos)\\[");

    private static final Map<String, String> TABLA_POR_CAMPO = Map.ofEntries(
            entry("codDiagnosticoPrincipal", Tablas.CIE10),
            entry("codDiagnosticoRelacionado", Tablas.CIE10),
            entry("codDiagnosticoRelacionado1", Tablas.CIE10),
            entry("codDiagnosticoRelacionado2", Tablas.CIE10),
            entry("codDiagnosticoRelacionado3", Tablas.CIE10),
            entry("codComplicacion", Tablas.CIE10),
            entry("codConsulta", Tablas.CUPS),
            entry("codProcedimiento", Tablas.CUPS),
            entry("codMunicipioResidencia", Tablas.MUNICIPIO),
            entry("codPaisResidencia", Tablas.PAIS),
            entry("codPaisOrigen", Tablas.PAIS));

    static UbicacionHallazgo de(String ruta) {
        Matcher u = USUARIO.matcher(ruta);
        Integer usuario = u.find() ? Integer.parseInt(u.group(1)) + 1 : null;
        Matcher s = SERVICIO.matcher(ruta);
        String tipo = s.find()
                ? ("consultas".equals(s.group(1)) ? "Consulta" : "Procedimiento")
                : (usuario != null ? "Usuario" : "Transacción");
        String campo = ruta.substring(ruta.lastIndexOf('.') + 1).replaceAll("\\[\\d+]$", "");
        return new UbicacionHallazgo(usuario, tipo, campo, TABLA_POR_CAMPO.get(campo));
    }
}
