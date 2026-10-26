package com.zseuz.rips.referencia;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee los CSV que escribe etl/descargar_tablas_sispro.py: coma como separador, comillas dobles
 * cuando el texto trae comas o comillas, primera fila con los nombres de columna.
 */
final class LectorCsv {

    static List<Map<String, String>> leer(BufferedReader lector) throws IOException {
        String encabezado = lector.readLine();
        if (encabezado == null) {
            return List.of();
        }
        List<String> columnas = campos(encabezado);
        List<Map<String, String>> filas = new ArrayList<>();
        for (String linea = lector.readLine(); linea != null; linea = lector.readLine()) {
            if (linea.isBlank()) {
                continue;
            }
            List<String> valores = campos(linea);
            Map<String, String> fila = new HashMap<>();
            for (int i = 0; i < columnas.size() && i < valores.size(); i++) {
                fila.put(columnas.get(i), valores.get(i));
            }
            filas.add(fila);
        }
        return filas;
    }

    static List<String> campos(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (entreComillas) {
                if (c == '"' && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else if (c == '"') {
                    entreComillas = false;
                } else {
                    actual.append(c);
                }
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == ',') {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos;
    }

    private LectorCsv() {
    }
}
