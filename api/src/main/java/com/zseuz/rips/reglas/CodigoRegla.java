package com.zseuz.rips.reglas;

import static com.zseuz.rips.reglas.Severidad.ADVERTENCIA;
import static com.zseuz.rips.reglas.Severidad.ERROR;

/**
 * Catálogo de reglas. El código es estable: los clientes (frontend, reportes de Power BI) lo usan
 * para agrupar hallazgos, así que no se renumera; una regla nueva toma un código nuevo.
 *
 * <ul>
 *   <li>RV00x: estructura, formato y tablas de referencia de SISPRO.
 *   <li>RV01x: transacción y usuarios.
 *   <li>RV02x: fechas y edad.
 *   <li>RV03x: coherencia clínica.
 *   <li>RV04x: valores monetarios.
 * </ul>
 */
public enum CodigoRegla {
    RV001(ERROR, "Campo obligatorio sin diligenciar"),
    RV002(ERROR, "Formato inválido"),
    RV003(ERROR, "Código que no existe en la tabla de referencia de SISPRO"),
    RV004(ERROR, "Fecha inválida o posterior a hoy"),
    RV005(ERROR, "Código deshabilitado en SISPRO"),
    RV006(ERROR, "CUPS que no corresponde al tipo de servicio"),

    RV010(ERROR, "Transacción sin usuarios"),
    RV011(ERROR, "Usuario repetido en la transacción"),
    RV012(ERROR, "Consecutivo fuera de secuencia"),
    RV013(ADVERTENCIA, "Usuario sin servicios reportados"),

    RV020(ADVERTENCIA, "Tipo de documento no corresponde a la edad"),
    RV021(ERROR, "Atención anterior a la fecha de nacimiento"),

    RV030(ERROR, "Diagnóstico que no aplica al sexo del usuario"),
    RV031(ADVERTENCIA, "Diagnóstico relacionado igual al principal"),
    RV032(ADVERTENCIA, "Diagnóstico fuera del rango de edad de la CIE-10"),

    RV040(ERROR, "Valor monetario negativo"),
    RV041(ERROR, "Pago moderador mayor que el valor del servicio"),
    RV042(ADVERTENCIA, "Servicio con valor cero");

    private final Severidad severidad;
    private final String titulo;

    CodigoRegla(Severidad severidad, String titulo) {
        this.severidad = severidad;
        this.titulo = titulo;
    }

    public Severidad severidad() {
        return severidad;
    }

    public String titulo() {
        return titulo;
    }
}
