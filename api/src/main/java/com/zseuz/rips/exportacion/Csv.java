package com.zseuz.rips.exportacion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;

/** Escribe CSV con coma y comillas dobles solo donde hacen falta (RFC 4180), como lo lee Power Query. */
final class Csv {

    private final Writer salida;

    Csv(Writer salida) {
        this.salida = salida;
    }

    void fila(Object... valores) {
        try {
            for (int i = 0; i < valores.length; i++) {
                if (i > 0) {
                    salida.write(',');
                }
                salida.write(celda(valores[i]));
            }
            salida.write("\r\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String celda(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.toString();
        boolean citar = texto.contains(",") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r");
        return citar ? '"' + texto.replace("\"", "\"\"") + '"' : texto;
    }
}
