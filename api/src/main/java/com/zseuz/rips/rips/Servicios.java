package com.zseuz.rips.rips;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Servicios prestados a un usuario, por tipo, tal como los agrupa el JSON de la Resolución 2275. */
public record Servicios(
        List<Consulta> consultas,
        List<Procedimiento> procedimientos,
        List<Urgencia> urgencias,
        List<Hospitalizacion> hospitalizacion,
        List<RecienNacido> recienNacidos,
        List<Medicamento> medicamentos,
        List<OtroServicio> otrosServicios) {

    static final Servicios VACIOS = new Servicios(null, null, null, null, null, null, null);

    public Servicios {
        consultas = consultas == null ? List.of() : consultas;
        procedimientos = procedimientos == null ? List.of() : procedimientos;
        urgencias = urgencias == null ? List.of() : urgencias;
        hospitalizacion = hospitalizacion == null ? List.of() : hospitalizacion;
        recienNacidos = recienNacidos == null ? List.of() : recienNacidos;
        medicamentos = medicamentos == null ? List.of() : medicamentos;
        otrosServicios = otrosServicios == null ? List.of() : otrosServicios;
    }

    /** Cada lista con el nombre de su campo en el JSON, en el orden del estándar. */
    public Map<String, List<? extends Atencion>> porTipo() {
        Map<String, List<? extends Atencion>> listas = new LinkedHashMap<>();
        listas.put("consultas", consultas);
        listas.put("procedimientos", procedimientos);
        listas.put("urgencias", urgencias);
        listas.put("hospitalizacion", hospitalizacion);
        listas.put("recienNacidos", recienNacidos);
        listas.put("medicamentos", medicamentos);
        listas.put("otrosServicios", otrosServicios);
        return listas;
    }

    public int total() {
        return porTipo().values().stream().mapToInt(List::size).sum();
    }
}
