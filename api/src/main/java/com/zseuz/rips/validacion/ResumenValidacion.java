package com.zseuz.rips.validacion;

import java.time.Instant;

/** Fila del historial: el resultado sin la lista de hallazgos. */
public record ResumenValidacion(
        Long id,
        String numFactura,
        String numDocumentoIdObligado,
        EstadoValidacion estado,
        Instant fechaValidacion,
        int totalUsuarios,
        int totalServicios,
        long totalErrores,
        long totalAdvertencias) {

    static ResumenValidacion de(Validacion v) {
        return new ResumenValidacion(v.getId(), v.getNumFactura(), v.getNumDocumentoIdObligado(), v.getEstado(),
                v.getFechaValidacion(), v.getTotalUsuarios(), v.getTotalServicios(), v.getTotalErrores(),
                v.getTotalAdvertencias());
    }
}
