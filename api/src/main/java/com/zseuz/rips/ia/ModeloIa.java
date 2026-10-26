package com.zseuz.rips.ia;

/** Un proveedor de IA que sabe explicar un hallazgo: Gemini o Claude. */
interface ModeloIa {

    /** Nombre del modelo, para mostrarlo junto a la explicación. */
    String nombre();

    /**
     * Pide la explicación con respuesta en JSON estructurado. Los errores del proveedor se lanzan
     * como {@code ResponseStatusException} con un mensaje que se puede mostrar al usuario.
     */
    Explicacion explicar(String instrucciones, String hallazgo);
}
