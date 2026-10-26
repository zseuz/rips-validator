package com.zseuz.rips.validacion;

import com.zseuz.rips.reglas.Hallazgo;
import com.zseuz.rips.reglas.Severidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Hallazgo guardado junto a su validación. */
@Embeddable
public class HallazgoValidacion {

    private static final int LARGO_VALOR = 255;

    @Column(length = 5, nullable = false)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private Severidad severidad;

    @Column(nullable = false)
    private String ruta;

    @Column(length = 500, nullable = false)
    private String mensaje;

    @Column(length = LARGO_VALOR)
    private String valor;

    protected HallazgoValidacion() {
    }

    HallazgoValidacion(Hallazgo hallazgo) {
        this.codigo = hallazgo.codigo();
        this.severidad = hallazgo.severidad();
        this.ruta = hallazgo.ruta();
        this.mensaje = hallazgo.mensaje();
        // El valor viene del archivo del cliente: se recorta para no fallar con datos basura.
        String recibido = hallazgo.valor();
        this.valor = recibido != null && recibido.length() > LARGO_VALOR ? recibido.substring(0, LARGO_VALOR) : recibido;
    }

    Hallazgo aHallazgo() {
        return new Hallazgo(codigo, severidad, ruta, mensaje, valor);
    }
}
