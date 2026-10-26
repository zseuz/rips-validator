package com.zseuz.rips.rips;

import java.math.BigDecimal;
import java.util.List;

/** Recién nacido atendido; se reporta dentro de los servicios de la madre. */
public record RecienNacido(
        String codPrestador,
        String tipoDocumentoIdentificacion,
        String numDocumentoIdentificacion,
        String fechaNacimiento,
        Integer edadGestacional,
        Integer numConsultasCPrenatal,
        String codSexoBiologico,
        BigDecimal peso,
        String codDiagnosticoPrincipal,
        String condicionDestinoUsuarioEgreso,
        String codDiagnosticoCausaMuerte,
        String fechaEgreso,
        Integer consecutivo) implements ConEgreso {

    @Override
    public String fechaAtencion() {
        return fechaNacimiento;
    }

    @Override
    public String campoFechaAtencion() {
        return "fechaNacimiento";
    }

    @Override
    public List<Campo> diagnosticosRelacionados() {
        return List.of();
    }
}
