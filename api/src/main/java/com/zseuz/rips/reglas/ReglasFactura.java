package com.zseuz.rips.reglas;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.zseuz.rips.factura.FacturaElectronica;
import com.zseuz.rips.rips.Atencion;
import com.zseuz.rips.rips.Cobrable;
import com.zseuz.rips.rips.Rips;
import com.zseuz.rips.rips.Usuario;

/**
 * Cruza el RIPS con la factura electrónica que soporta. Solo corre cuando se adjunta el XML, por
 * eso no es un {@link ReglaRips}: {@link ValidadorRips} la llama aparte.
 */
final class ReglasFactura {

    static void validar(Rips rips, FacturaElectronica factura, Hallazgos hallazgos) {
        if (factura.numero() != null && rips.numFactura() != null && !factura.numero().equals(rips.numFactura())) {
            hallazgos.agregar(CodigoRegla.RV050, "numFactura",
                    "La factura electrónica adjunta es la " + factura.numero(), rips.numFactura());
        }
        if (factura.nitEmisor() != null && rips.numDocumentoIdObligado() != null
                && !factura.nitEmisor().equals(rips.numDocumentoIdObligado())) {
            hallazgos.agregar(CodigoRegla.RV051, "numDocumentoIdObligado",
                    "La factura la emite el NIT " + factura.nitEmisor(), rips.numDocumentoIdObligado());
        }

        BigDecimal totalRips = BigDecimal.ZERO;
        LocalDateTime ultimaAtencion = null;
        String rutaUltima = null;
        List<Usuario> usuarios = rips.usuarios();
        for (int u = 0; u < usuarios.size(); u++) {
            for (Map.Entry<String, List<? extends Atencion>> lista : usuarios.get(u).servicios().porTipo().entrySet()) {
                for (int i = 0; i < lista.getValue().size(); i++) {
                    Atencion atencion = lista.getValue().get(i);
                    if (atencion instanceof Cobrable cobrable && cobrable.vrServicio() != null) {
                        totalRips = totalRips.add(cobrable.vrServicio());
                    }
                    LocalDateTime fecha = Fechas.leerFechaHora(atencion.fechaAtencion()).orElse(null);
                    if (fecha != null && (ultimaAtencion == null || fecha.isAfter(ultimaAtencion))) {
                        ultimaAtencion = fecha;
                        rutaUltima = "usuarios[" + u + "].servicios." + lista.getKey() + "[" + i + "]." + atencion.campoFechaAtencion();
                    }
                }
            }
        }

        if (factura.valorBruto() != null && totalRips.compareTo(factura.valorBruto()) != 0) {
            hallazgos.agregar(CodigoRegla.RV052, "factura.LineExtensionAmount",
                    "La suma de vrServicio del RIPS es " + totalRips.toPlainString() + " y el valor bruto de la factura es "
                            + factura.valorBruto().toPlainString() + "; revise servicios faltantes o copagos descontados",
                    factura.valorBruto());
        }
        if (factura.fechaEmision() != null && ultimaAtencion != null && ultimaAtencion.toLocalDate().isAfter(factura.fechaEmision())) {
            hallazgos.agregar(CodigoRegla.RV053, rutaUltima,
                    "La factura se emitió el " + factura.fechaEmision() + ", antes de esta atención", ultimaAtencion.toString().replace('T', ' '));
        }
    }

    private ReglasFactura() {
    }
}
