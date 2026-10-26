package com.zseuz.rips.reglas;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Valores y formatos de los campos que no tienen tabla de referencia en SISPRO. Los que sí la
 * tienen (sexo, tipos de documento, CIE-10, CUPS, municipios…) se validan contra
 * {@link com.zseuz.rips.referencia.TablasReferencia}.
 */
final class Catalogos {

    static final Set<String> SI_NO = Set.of("SI", "NO");

    /** Nota "RIPS sin facturación": es la única que no lleva número de factura. */
    static final String NOTA_SIN_FACTURA = "RS";
    static final String PAIS_COLOMBIA = "170";

    static final Pattern DOCUMENTO_OBLIGADO = Pattern.compile("\\d{5,20}");
    static final Pattern NUMERO_FACTURA = Pattern.compile("[A-Za-z0-9-]{1,20}");
    static final Pattern NUMERO_DOCUMENTO = Pattern.compile("[A-Za-z0-9]{1,20}");
    /** Código de habilitación del REPS: 10 dígitos del prestador + 2 de la sede. */
    static final Pattern CODIGO_PRESTADOR = Pattern.compile("\\d{12}");

    private Catalogos() {
    }
}
