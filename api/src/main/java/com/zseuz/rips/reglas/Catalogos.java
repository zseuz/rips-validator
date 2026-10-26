package com.zseuz.rips.reglas;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Valores y formatos permitidos.
 *
 * <p>Los catálogos son un <b>subconjunto</b> de las tablas de referencia de SISPRO, suficiente
 * para los casos comunes. Antes de usar el validador en producción hay que cargarlos desde las
 * tablas oficiales vigentes, que el Ministerio actualiza.
 */
final class Catalogos {

    static final Set<String> TIPOS_DOCUMENTO =
            Set.of("CC", "CE", "CD", "PA", "SC", "PE", "RC", "TI", "CN", "AS", "MS", "DE", "PT", "SI");
    static final Set<String> SEXOS = Set.of("H", "M", "I");
    static final Set<String> ZONAS_TERRITORIALES = Set.of("01", "02");
    static final Set<String> SI_NO = Set.of("SI", "NO");
    static final Set<String> TIPOS_NOTA = Set.of("NA", "NC", "ND", "RS");
    static final Set<String> CONCEPTOS_RECAUDO = Set.of("01", "02", "03", "04", "05");
    static final Set<String> TIPOS_DIAGNOSTICO_PRINCIPAL = Set.of("01", "02", "03");

    /** Nota "RIPS sin facturación": es la única que no lleva número de factura. */
    static final String NOTA_SIN_FACTURA = "RS";
    static final String PAIS_COLOMBIA = "170";

    static final Pattern DOCUMENTO_OBLIGADO = Pattern.compile("\\d{5,20}");
    static final Pattern NUMERO_FACTURA = Pattern.compile("[A-Za-z0-9-]{1,20}");
    static final Pattern NUMERO_DOCUMENTO = Pattern.compile("[A-Za-z0-9]{1,20}");
    static final Pattern TIPO_USUARIO = Pattern.compile("\\d{2}");
    static final Pattern PAIS = Pattern.compile("\\d{3}");
    static final Pattern MUNICIPIO = Pattern.compile("\\d{5}");
    /** Código de habilitación del REPS: 10 dígitos del prestador + 2 de la sede. */
    static final Pattern CODIGO_PRESTADOR = Pattern.compile("\\d{12}");
    static final Pattern CUPS = Pattern.compile("[0-9A-Z]{6}");
    /** CIE-10 como se usa en Colombia: letra, dos dígitos y un cuarto carácter opcional (J069). */
    static final Pattern CIE10 = Pattern.compile("[A-Z]\\d{2}[0-9A-Z]?");

    /** Capítulo O (embarazo, parto y puerperio): solo aplica a mujeres. */
    static final String PREFIJO_SOLO_MUJER = "O";
    /** Próstata: tumor maligno, hiperplasia y enfermedad inflamatoria. */
    static final Set<String> CATEGORIAS_SOLO_HOMBRE = Set.of("C61", "N40", "N41");

    private Catalogos() {
    }
}
