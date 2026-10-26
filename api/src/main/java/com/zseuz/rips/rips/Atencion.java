package com.zseuz.rips.rips;

import java.util.List;

/** Lo que comparten todos los tipos de servicio del RIPS: prestador, fecha, diagnósticos y consecutivo. */
public interface Atencion {

    String codPrestador();

    Integer consecutivo();

    /** Fecha y hora de la atención: inicio, dispensación, suministro o nacimiento según el tipo. */
    String fechaAtencion();

    /** Nombre del campo JSON que trae {@link #fechaAtencion()}, para armar la ruta del hallazgo. */
    String campoFechaAtencion();

    /** Diagnóstico principal CIE-10; {@code null} en los tipos que no lo llevan (otros servicios). */
    String codDiagnosticoPrincipal();

    default boolean requiereDiagnostico() {
        return true;
    }

    /** Demás diagnósticos CIE-10 (relacionados, de egreso, complicación), con su nombre de campo. */
    List<Campo> diagnosticosRelacionados();

    record Campo(String nombre, String valor) {
    }
}
