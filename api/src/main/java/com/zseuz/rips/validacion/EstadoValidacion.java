package com.zseuz.rips.validacion;

public enum EstadoValidacion {
    VALIDO,
    VALIDO_CON_ADVERTENCIAS,
    RECHAZADO;

    static EstadoValidacion segun(long errores, long advertencias) {
        if (errores > 0) {
            return RECHAZADO;
        }
        return advertencias > 0 ? VALIDO_CON_ADVERTENCIAS : VALIDO;
    }
}
