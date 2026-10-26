package com.zseuz.rips.rips;

import java.math.BigDecimal;
import java.util.List;

/** Dispositivos médicos, traslados, estancias y servicios complementarios. No lleva diagnóstico. */
public record OtroServicio(
        String codPrestador,
        String numAutorizacion,
        String idMIPRES,
        String fechaSuministroTecnologia,
        String tipoOS,
        String codTecnologiaSalud,
        String nomTecnologiaSalud,
        BigDecimal cantidadOS,
        String tipoDocumentoIdentificacion,
        String numDocumentoIdentificacion,
        BigDecimal vrUnitOS,
        BigDecimal vrServicio,
        String conceptoRecaudo,
        BigDecimal valorPagoModerador,
        String numFEVPagoModerador,
        Integer consecutivo) implements Cobrable {

    @Override
    public String fechaAtencion() {
        return fechaSuministroTecnologia;
    }

    @Override
    public String campoFechaAtencion() {
        return "fechaSuministroTecnologia";
    }

    @Override
    public String codDiagnosticoPrincipal() {
        return null;
    }

    @Override
    public boolean requiereDiagnostico() {
        return false;
    }

    @Override
    public List<Campo> diagnosticosRelacionados() {
        return List.of();
    }
}
