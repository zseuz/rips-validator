package com.zseuz.rips.reglas;

import com.zseuz.rips.rips.Rips;

/**
 * Un grupo de reglas sobre la transacción. Cada implementación es un bean de Spring y
 * {@code ValidadorRips} las ejecuta todas en el orden de {@code @Order}; para añadir reglas basta
 * con crear otra clase.
 */
public interface ReglaRips {

    void validar(Rips rips, Hallazgos hallazgos);
}
