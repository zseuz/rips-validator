package com.zseuz.rips.factura;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Los datos de la factura electrónica de venta (XML UBL 2.1 de la DIAN) que se cruzan con el RIPS.
 *
 * @param valorBruto {@code cac:LegalMonetaryTotal/cbc:LineExtensionAmount}: total de las líneas antes de impuestos
 */
public record FacturaElectronica(String numero, String nitEmisor, LocalDate fechaEmision, BigDecimal valorBruto) {
}
