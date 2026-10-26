package com.zseuz.rips.rips;

import java.util.List;

public record Hospitalizacion(
        String codPrestador,
        String viaIngresoServicioSalud,
        String fechaInicioAtencion,
        String numAutorizacion,
        String causaMotivoAtencion,
        String codDiagnosticoPrincipal,
        String codDiagnosticoPrincipalE,
        String codDiagnosticoRelacionadoE1,
        String codDiagnosticoRelacionadoE2,
        String codDiagnosticoRelacionadoE3,
        String codComplicacion,
        String condicionDestinoUsuarioEgreso,
        String codDiagnosticoCausaMuerte,
        String fechaEgreso,
        Integer consecutivo) implements ConEgreso {

    @Override
    public String fechaAtencion() {
        return fechaInicioAtencion;
    }

    @Override
    public String campoFechaAtencion() {
        return "fechaInicioAtencion";
    }

    @Override
    public List<Campo> diagnosticosRelacionados() {
        return List.of(
                new Campo("codDiagnosticoPrincipalE", codDiagnosticoPrincipalE),
                new Campo("codDiagnosticoRelacionadoE1", codDiagnosticoRelacionadoE1),
                new Campo("codDiagnosticoRelacionadoE2", codDiagnosticoRelacionadoE2),
                new Campo("codDiagnosticoRelacionadoE3", codDiagnosticoRelacionadoE3),
                new Campo("codComplicacion", codComplicacion));
    }
}
