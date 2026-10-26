package com.zseuz.rips.rips;

import java.util.List;

/**
 * Transacción RIPS en JSON tal como la define la Resolución 2275 de 2023: una factura
 * electrónica (o nota) con los usuarios atendidos y sus servicios.
 *
 * <p>Los campos se reciben como texto aunque el estándar los defina como fechas o códigos: así un
 * valor mal formado no rompe la lectura del archivo y se puede reportar como hallazgo con su ruta
 * exacta. Los valores monetarios sí llegan como números.
 */
public record Rips(
        String numDocumentoIdObligado,
        String numFactura,
        String tipoNota,
        String numNota,
        List<Usuario> usuarios) {

    public List<Usuario> usuarios() {
        return usuarios == null ? List.of() : usuarios;
    }
}
