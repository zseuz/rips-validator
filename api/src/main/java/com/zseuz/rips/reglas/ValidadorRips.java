package com.zseuz.rips.reglas;

import java.util.List;

import org.springframework.stereotype.Component;

import com.zseuz.rips.rips.Rips;

/** Ejecuta todas las reglas registradas sobre una transacción. No guarda nada. */
@Component
public class ValidadorRips {

    private final List<ReglaRips> reglas;

    public ValidadorRips(List<ReglaRips> reglas) {
        this.reglas = reglas;
    }

    public List<Hallazgo> validar(Rips rips) {
        Hallazgos hallazgos = new Hallazgos();
        reglas.forEach(regla -> regla.validar(rips, hallazgos));
        return hallazgos.lista();
    }
}
