package com.zseuz.rips.reglas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Acumula los hallazgos de una validación y ofrece las comprobaciones de campo que repiten todas
 * las reglas (obligatorio, formato, catálogo, fecha).
 */
public final class Hallazgos {

    private final List<Hallazgo> lista = new ArrayList<>();

    public void agregar(CodigoRegla regla, String ruta, String mensaje, Object valor) {
        lista.add(new Hallazgo(regla, ruta, mensaje, valor));
    }

    public List<Hallazgo> lista() {
        return List.copyOf(lista);
    }

    /** Devuelve {@code true} si el campo trae valor; si no, registra RV001. */
    boolean obligatorio(String ruta, Object valor) {
        if (valor == null || valor instanceof String texto && texto.isBlank()) {
            agregar(CodigoRegla.RV001, ruta, "El campo es obligatorio", null);
            return false;
        }
        return true;
    }

    /** Comprueba el formato solo si el campo trae valor; los vacíos los reporta {@link #obligatorio}. */
    void formato(String ruta, String valor, Pattern patron, String esperado) {
        if (valor != null && !valor.isBlank() && !patron.matcher(valor).matches()) {
            agregar(CodigoRegla.RV002, ruta, "Se esperaba " + esperado, valor);
        }
    }

    void catalogo(String ruta, String valor, Set<String> permitidos) {
        if (valor != null && !valor.isBlank() && !permitidos.contains(valor)) {
            agregar(CodigoRegla.RV003, ruta, "Valores permitidos: " + String.join(", ", permitidos.stream().sorted().toList()), valor);
        }
    }

    /** Lee una fecha {@code AAAA-MM-DD} que no puede ser posterior a {@code hoy}; registra RV004 si no cumple. */
    Optional<LocalDate> fecha(String ruta, String valor, LocalDate hoy) {
        return validarFecha(ruta, valor, Fechas.leerFecha(valor), "AAAA-MM-DD", hoy);
    }

    /** Igual que {@link #fecha}, para fechas con hora {@code AAAA-MM-DD HH:MM}. */
    Optional<LocalDate> fechaHora(String ruta, String valor, LocalDate hoy) {
        return validarFecha(ruta, valor, Fechas.leerFechaHora(valor).map(f -> f.toLocalDate()), "AAAA-MM-DD HH:MM", hoy);
    }

    private Optional<LocalDate> validarFecha(String ruta, String valor, Optional<LocalDate> leida, String formato, LocalDate hoy) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        if (leida.isEmpty()) {
            agregar(CodigoRegla.RV004, ruta, "Fecha inexistente o con formato distinto de " + formato, valor);
        } else if (leida.get().isAfter(hoy)) {
            agregar(CodigoRegla.RV004, ruta, "La fecha es posterior a hoy (" + hoy + ")", valor);
            return Optional.empty();
        }
        return leida;
    }
}
