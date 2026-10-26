package com.zseuz.rips.rips;

public record Usuario(
        String tipoDocumentoIdentificacion,
        String numDocumentoIdentificacion,
        String tipoUsuario,
        String fechaNacimiento,
        String codSexo,
        String codPaisResidencia,
        String codMunicipioResidencia,
        String codZonaTerritorialResidencia,
        String incapacidad,
        Integer consecutivo,
        String codPaisOrigen,
        Servicios servicios) {

    public Servicios servicios() {
        return servicios == null ? Servicios.VACIOS : servicios;
    }
}
