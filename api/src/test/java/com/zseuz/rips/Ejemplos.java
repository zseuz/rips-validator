package com.zseuz.rips;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Archivos de {@code /ejemplos} (raíz del repositorio) y reloj fijo compartidos por las pruebas. */
public final class Ejemplos {

    /** Las pruebas corren "hoy" en esta fecha para que las reglas de edad y de fechas futuras no cambien con los años. */
    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-26T15:00:00Z"), ZoneId.of("America/Bogota"));

    public static String leer(String nombre) {
        try {
            // Maven ejecuta las pruebas desde la carpeta api/.
            return Files.readString(Path.of("..", "ejemplos", nombre));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Ejemplos() {
    }
}
