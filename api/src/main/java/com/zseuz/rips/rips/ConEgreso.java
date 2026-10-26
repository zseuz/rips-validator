package com.zseuz.rips.rips;

/** Atenciones que terminan con un egreso: urgencias, hospitalización y recién nacidos. */
public interface ConEgreso extends Atencion {

    String fechaEgreso();

    String condicionDestinoUsuarioEgreso();

    String codDiagnosticoCausaMuerte();
}
