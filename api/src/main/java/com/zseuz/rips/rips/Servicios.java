package com.zseuz.rips.rips;

import java.util.List;

/**
 * Servicios prestados a un usuario. Por ahora se validan consultas y procedimientos; el resto de
 * tipos del estándar (urgencias, hospitalización, recién nacidos, medicamentos y otros servicios)
 * se ignoran al leer el JSON.
 */
public record Servicios(List<Consulta> consultas, List<Procedimiento> procedimientos) {

    static final Servicios VACIOS = new Servicios(List.of(), List.of());

    public List<Consulta> consultas() {
        return consultas == null ? List.of() : consultas;
    }

    public List<Procedimiento> procedimientos() {
        return procedimientos == null ? List.of() : procedimientos;
    }

    public int total() {
        return consultas().size() + procedimientos().size();
    }
}
