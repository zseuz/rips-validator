package com.zseuz.rips.reglas;

public enum Severidad {
    /** El Ministerio rechazaría el archivo: hay que corregirlo antes de enviarlo. */
    ERROR,
    /** No impide el envío, pero suele indicar un dato mal capturado o una glosa futura. */
    ADVERTENCIA
}
