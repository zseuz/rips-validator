package com.zseuz.rips.ia;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Respuesta estructurada del modelo: el SDK deriva el esquema JSON de este record. */
public record Explicacion(
        @JsonPropertyDescription("Qué significa el hallazgo, en una o dos frases sencillas, sin jerga técnica innecesaria")
        String queSignifica,
        @JsonPropertyDescription("Pasos concretos para corregirlo en el archivo o en el sistema que genera el RIPS")
        String comoCorregirlo,
        @JsonPropertyDescription("Qué pasa si no se corrige: rechazo del Ministerio, glosa de la EPS o riesgo de auditoría")
        String impacto) {
}
