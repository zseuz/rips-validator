package com.zseuz.rips.reglas;

/**
 * Un problema encontrado en el RIPS.
 *
 * @param ruta ubicación del campo en el JSON, p. ej. {@code usuarios[0].servicios.consultas[2].codConsulta}
 * @param valor el valor que se recibió, o {@code null} si el campo venía vacío
 */
public record Hallazgo(String codigo, Severidad severidad, String ruta, String mensaje, String valor) {

    public Hallazgo(CodigoRegla regla, String ruta, String mensaje, Object valor) {
        this(regla.name(), regla.severidad(), ruta, mensaje, valor == null ? null : valor.toString());
    }
}
