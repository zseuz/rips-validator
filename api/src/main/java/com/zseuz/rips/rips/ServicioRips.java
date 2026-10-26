package com.zseuz.rips.rips;

/**
 * Consultas y procedimientos: servicios identificados con un código CUPS. Comparten las reglas de
 * CUPS, modalidad, grupo de servicios y finalidad.
 */
public interface ServicioRips extends Cobrable {

    String fechaInicioAtencion();

    String modalidadGrupoServicioTecSal();

    String grupoServicios();

    String finalidadTecnologiaSalud();

    /** Código CUPS de la consulta o del procedimiento. */
    String codigoTecnologia();

    /** Nombre del campo JSON que trae el código CUPS, para armar la ruta del hallazgo. */
    String campoCodigoTecnologia();

    @Override
    default String fechaAtencion() {
        return fechaInicioAtencion();
    }

    @Override
    default String campoFechaAtencion() {
        return "fechaInicioAtencion";
    }
}
