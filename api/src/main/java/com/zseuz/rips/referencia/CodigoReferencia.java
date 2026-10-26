package com.zseuz.rips.referencia;

/**
 * Un código de una tabla de referencia de SISPRO.
 *
 * @param sexo {@code H} o {@code M} si el código solo aplica a ese sexo (CIE-10); {@code null} si aplica a ambos
 * @param edadMinima edad mínima en años (CIE-10); {@code null} si la tabla no la define
 * @param edadMaxima edad máxima en años (CIE-10); 999 significa sin límite
 * @param uso {@code AC} si el CUPS se reporta como consulta, {@code AP} si es procedimiento
 */
public record CodigoReferencia(
        String tabla,
        String codigo,
        String nombre,
        boolean habilitado,
        String sexo,
        Integer edadMinima,
        Integer edadMaxima,
        String uso) {

    public String etiqueta() {
        return codigo + " (" + nombre + ")";
    }
}
