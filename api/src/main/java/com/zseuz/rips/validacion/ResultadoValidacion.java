package com.zseuz.rips.validacion;

import java.time.Instant;
import java.util.List;

import com.zseuz.rips.reglas.Hallazgo;

public record ResultadoValidacion(
        Long id,
        String numFactura,
        String numDocumentoIdObligado,
        EstadoValidacion estado,
        Instant fechaValidacion,
        int totalUsuarios,
        int totalServicios,
        long totalErrores,
        long totalAdvertencias,
        List<Hallazgo> hallazgos) {

    static ResultadoValidacion de(Validacion v) {
        return new ResultadoValidacion(v.getId(), v.getNumFactura(), v.getNumDocumentoIdObligado(), v.getEstado(),
                v.getFechaValidacion(), v.getTotalUsuarios(), v.getTotalServicios(), v.getTotalErrores(),
                v.getTotalAdvertencias(), v.getHallazgos());
    }
}
