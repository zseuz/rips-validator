package com.zseuz.rips.reglas;

import java.util.List;

import org.springframework.stereotype.Component;

import com.zseuz.rips.referencia.TablasReferencia;
import com.zseuz.rips.rips.Rips;

/** Ejecuta todas las reglas registradas sobre una transacción. No guarda nada. */
@Component
public class ValidadorRips {

    private final List<ReglaRips> reglas;
    private final TablasReferencia tablas;

    public ValidadorRips(List<ReglaRips> reglas, TablasReferencia tablas) {
        this.reglas = reglas;
        this.tablas = tablas;
    }

    public List<Hallazgo> validar(Rips rips) {
        Hallazgos hallazgos = new Hallazgos(tablas);
        reglas.forEach(regla -> regla.validar(rips, hallazgos));
        return hallazgos.lista();
    }
}
