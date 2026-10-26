package com.zseuz.rips.reglas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.zseuz.rips.referencia.CodigoReferencia;
import com.zseuz.rips.referencia.TablasReferencia;

/**
 * Acumula los hallazgos de una validación y ofrece las comprobaciones de campo que repiten todas
 * las reglas (obligatorio, formato, catálogo, fecha).
 */
public final class Hallazgos {

    private final List<Hallazgo> lista = new ArrayList<>();
    private final TablasReferencia tablas;

    Hallazgos(TablasReferencia tablas) {
        this.tablas = tablas;
    }

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

    /**
     * Comprueba el código contra una tabla de referencia de SISPRO: si no existe registra RV003 y si
     * está deshabilitado, RV005. Devuelve el código encontrado para reglas que usan sus atributos.
     */
    Optional<CodigoReferencia> referencia(String ruta, String valor, String tabla) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        Optional<CodigoReferencia> codigo = tablas.buscar(tabla, valor);
        if (codigo.isEmpty() && tablas.tiene(tabla)) {
            agregar(CodigoRegla.RV003, ruta, "No existe en la tabla " + tabla + " de SISPRO", valor);
        } else if (codigo.isPresent() && !codigo.get().habilitado()) {
            agregar(CodigoRegla.RV005, ruta, codigo.get().etiqueta() + " está deshabilitado en la tabla " + tabla + " de SISPRO", valor);
        }
        return codigo;
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
