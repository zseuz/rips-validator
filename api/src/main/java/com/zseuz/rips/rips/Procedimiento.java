package com.zseuz.rips.rips;

import java.math.BigDecimal;
import java.util.List;

public record Procedimiento(
        String codPrestador,
        String fechaInicioAtencion,
        String idMIPRES,
        String numAutorizacion,
        String codProcedimiento,
        String viaIngresoServicioSalud,
        String modalidadGrupoServicioTecSal,
        String grupoServicios,
        Integer codServicio,
        String finalidadTecnologiaSalud,
        String tipoDocumentoIdentificacion,
        String numDocumentoIdentificacion,
        String codDiagnosticoPrincipal,
        String codDiagnosticoRelacionado,
        String codComplicacion,
        BigDecimal vrServicio,
        String conceptoRecaudo,
        BigDecimal valorPagoModerador,
        String numFEVPagoModerador,
        Integer consecutivo) implements ServicioRips {

    @Override
    public String codigoTecnologia() {
        return codProcedimiento;
    }

    @Override
    public String campoCodigoTecnologia() {
        return "codProcedimiento";
    }

    @Override
    public List<Campo> diagnosticosRelacionados() {
        return List.of(
                new Campo("codDiagnosticoRelacionado", codDiagnosticoRelacionado),
                new Campo("codComplicacion", codComplicacion));
    }
}
