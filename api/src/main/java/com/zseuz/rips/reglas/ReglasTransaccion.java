package com.zseuz.rips.reglas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.zseuz.rips.referencia.Tablas;
import com.zseuz.rips.rips.Rips;
import com.zseuz.rips.rips.Usuario;

/** Encabezado de la transacción y la lista de usuarios como conjunto. */
@Component
@Order(1)
class ReglasTransaccion implements ReglaRips {

    @Override
    public void validar(Rips rips, Hallazgos hallazgos) {
        if (hallazgos.obligatorio("numDocumentoIdObligado", rips.numDocumentoIdObligado())) {
            hallazgos.formato("numDocumentoIdObligado", rips.numDocumentoIdObligado(), Catalogos.DOCUMENTO_OBLIGADO,
                    "solo dígitos (NIT sin dígito de verificación)");
        }
        hallazgos.referencia("tipoNota", rips.tipoNota(), Tablas.TIPO_NOTA);
        if (!Catalogos.NOTA_SIN_FACTURA.equals(rips.tipoNota())
                && hallazgos.obligatorio("numFactura", rips.numFactura())) {
            hallazgos.formato("numFactura", rips.numFactura(), Catalogos.NUMERO_FACTURA,
                    "hasta 20 letras, dígitos o guiones");
        }

        List<Usuario> usuarios = rips.usuarios();
        if (usuarios.isEmpty()) {
            hallazgos.agregar(CodigoRegla.RV010, "usuarios", "La transacción debe reportar al menos un usuario", null);
            return;
        }
        Map<String, Integer> vistos = new HashMap<>();
        for (int i = 0; i < usuarios.size(); i++) {
            Usuario usuario = usuarios.get(i);
            String ruta = "usuarios[" + i + "]";
            if (usuario.numDocumentoIdentificacion() != null) {
                String documento = usuario.tipoDocumentoIdentificacion() + " " + usuario.numDocumentoIdentificacion();
                Integer anterior = vistos.putIfAbsent(documento, i);
                if (anterior != null) {
                    hallazgos.agregar(CodigoRegla.RV011, ruta,
                            "El usuario ya aparece en usuarios[" + anterior + "]; sus servicios deben ir en un solo registro", documento);
                }
            }
            if (usuario.consecutivo() == null || usuario.consecutivo() != i + 1) {
                hallazgos.agregar(CodigoRegla.RV012, ruta + ".consecutivo", "Se esperaba " + (i + 1), usuario.consecutivo());
            }
        }
    }
}
