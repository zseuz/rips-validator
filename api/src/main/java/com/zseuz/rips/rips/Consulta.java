package com.zseuz.rips.rips;

import java.math.BigDecimal;
import java.util.List;

public record Consulta(
        String codPrestador,
        String fechaInicioAtencion,
        String numAutorizacion,
        String codConsulta,
        String modalidadGrupoServicioTecSal,
        String grupoServicios,
        Integer codServicio,
        String finalidadTecnologiaSalud,
        String causaMotivoAtencion,
        String codDiagnosticoPrincipal,
        String codDiagnosticoRelacionado1,
        String codDiagnosticoRelacionado2,
        String codDiagnosticoRelacionado3,
        String tipoDiagnosticoPrincipal,
        String tipoDocumentoIdentificacion,
        String numDocumentoIdentificacion,
        BigDecimal vrServicio,
        String conceptoRecaudo,
        BigDecimal valorPagoModerador,
        String numFEVPagoModerador,
        Integer consecutivo) implements ServicioRips {

    @Override
    public String codigoTecnologia() {
        return codConsulta;
    }

    @Override
    public String campoCodigoTecnologia() {
        return "codConsulta";
    }

    @Override
    public List<Campo> diagnosticosRelacionados() {
        return List.of(
                new Campo("codDiagnosticoRelacionado1", codDiagnosticoRelacionado1),
                new Campo("codDiagnosticoRelacionado2", codDiagnosticoRelacionado2),
                new Campo("codDiagnosticoRelacionado3", codDiagnosticoRelacionado3));
    }
}
