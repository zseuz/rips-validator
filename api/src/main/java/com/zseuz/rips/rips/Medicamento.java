package com.zseuz.rips.rips;

import java.math.BigDecimal;
import java.util.List;

public record Medicamento(
        String codPrestador,
        String numAutorizacion,
        String idMIPRES,
        String fechaDispensAdmon,
        String codDiagnosticoPrincipal,
        String codDiagnosticoRelacionado,
        String tipoMedicamento,
        String codTecnologiaSalud,
        String nomTecnologiaSalud,
        BigDecimal concentracionMedicamento,
        Integer unidadMedida,
        String formaFarmaceutica,
        Integer unidadMinDispensa,
        BigDecimal cantidadMedicamento,
        Integer diasTratamiento,
        String tipoDocumentoIdentificacion,
        String numDocumentoIdentificacion,
        BigDecimal vrUnitMedicamento,
        BigDecimal vrServicio,
        String conceptoRecaudo,
        BigDecimal valorPagoModerador,
        String numFEVPagoModerador,
        Integer consecutivo) implements Cobrable {

    @Override
    public String fechaAtencion() {
        return fechaDispensAdmon;
    }

    @Override
    public String campoFechaAtencion() {
        return "fechaDispensAdmon";
    }

    @Override
    public List<Campo> diagnosticosRelacionados() {
        return List.of(new Campo("codDiagnosticoRelacionado", codDiagnosticoRelacionado));
    }
}
