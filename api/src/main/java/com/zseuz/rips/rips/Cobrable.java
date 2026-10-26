package com.zseuz.rips.rips;

import java.math.BigDecimal;

/** Servicios con valor: el profesional que atiende, el valor y el pago moderador. */
public interface Cobrable extends Atencion {

    String tipoDocumentoIdentificacion();

    String numDocumentoIdentificacion();

    BigDecimal vrServicio();

    String conceptoRecaudo();

    BigDecimal valorPagoModerador();
}
