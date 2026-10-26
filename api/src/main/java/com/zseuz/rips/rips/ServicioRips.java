package com.zseuz.rips.rips;

import java.math.BigDecimal;
import java.util.List;

/**
 * Campos que comparten todos los tipos de servicio. Permite validar consultas y procedimientos
 * con las mismas reglas sin duplicarlas.
 */
public interface ServicioRips {

    String codPrestador();

    String fechaInicioAtencion();

    String codDiagnosticoPrincipal();

    String tipoDocumentoIdentificacion();

    String numDocumentoIdentificacion();

    BigDecimal vrServicio();

    String conceptoRecaudo();

    BigDecimal valorPagoModerador();

    Integer consecutivo();

    /** Código CUPS de la consulta o del procedimiento. */
    String codigoTecnologia();

    /** Nombre del campo JSON que trae el código CUPS, para armar la ruta del hallazgo. */
    String campoCodigoTecnologia();

    /** Diagnósticos relacionados en el orden del JSON, con su nombre de campo. */
    List<Campo> diagnosticosRelacionados();

    record Campo(String nombre, String valor) {
    }
}
