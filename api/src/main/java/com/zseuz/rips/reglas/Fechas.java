package com.zseuz.rips.reglas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;

/** Formatos de fecha del estándar. Estrictos: rechazan días como 2026-02-30. */
final class Fechas {

    static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

    static Optional<LocalDate> leerFecha(String valor) {
        try {
            return valor == null ? Optional.empty() : Optional.of(LocalDate.parse(valor, FECHA));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    static Optional<LocalDateTime> leerFechaHora(String valor) {
        try {
            return valor == null ? Optional.empty() : Optional.of(LocalDateTime.parse(valor, FECHA_HORA));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private Fechas() {
    }
}
