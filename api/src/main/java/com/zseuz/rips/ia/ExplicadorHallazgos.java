package com.zseuz.rips.ia;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.zseuz.rips.reglas.CodigoRegla;
import com.zseuz.rips.reglas.Hallazgo;

/**
 * Explica un hallazgo en lenguaje sencillo con el proveedor de IA configurado (Gemini o Claude).
 * Las explicaciones se guardan en memoria por validación y hallazgo: pedir dos veces la misma no
 * vuelve a llamar (ni a cobrar) la API.
 */
@Service
public class ExplicadorHallazgos {

    private static final String INSTRUCCIONES = """
            Eres un auditor de cuentas médicas en Colombia experto en RIPS (Resolución 2275 de 2023), factura \
            electrónica de venta en salud y las tablas de referencia de SISPRO. Un validador automático encontró un \
            hallazgo en un archivo RIPS en JSON. Explícaselo a la persona de facturación de una IPS que no es \
            programadora: qué significa, cómo corregirlo y qué pasa si no lo corrige. Sé concreto y breve (máximo \
            tres frases por campo), en español de Colombia. Si el hallazgo es una advertencia, dilo: no bloquea el \
            envío pero puede terminar en glosa. No inventes normas ni números de artículo.""";

    /** Campos con datos personales: su valor no se envía a la API. */
    private static final Set<String> CAMPOS_PERSONALES = Set.of("numDocumentoIdentificacion", "fechaNacimiento", "usuarios");

    private final Optional<ModeloIa> modelo;
    private final Map<String, Explicacion> cache = new ConcurrentHashMap<>();

    ExplicadorHallazgos(IaConfig.ProveedorIa proveedor) {
        this.modelo = proveedor.modelo();
    }

    public boolean disponible() {
        return modelo.isPresent();
    }

    public String modelo() {
        return modelo.map(ModeloIa::nombre).orElse(null);
    }

    public Explicacion explicar(long validacion, int orden, Hallazgo hallazgo) {
        ModeloIa ia = modelo.orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "La explicación con IA no está configurada: falta GEMINI_API_KEY o ANTHROPIC_API_KEY en la API"));
        return cache.computeIfAbsent(validacion + "/" + orden, clave -> ia.explicar(INSTRUCCIONES, describir(hallazgo)));
    }

    /** El hallazgo como texto para el modelo, sin datos personales del usuario. */
    static String describir(Hallazgo hallazgo) {
        String campo = hallazgo.ruta().substring(hallazgo.ruta().lastIndexOf('.') + 1).replaceAll("\\[\\d+]$", "");
        String titulo = Optional.of(hallazgo.codigo()).map(CodigoRegla::valueOf).map(CodigoRegla::titulo).orElse("");
        String valor = hallazgo.valor() == null ? "(vacío)"
                : CAMPOS_PERSONALES.contains(campo) ? "(omitido: dato personal)" : hallazgo.valor();
        return """
                Regla: %s – %s
                Severidad: %s
                Campo: %s (ruta en el JSON: %s)
                Valor recibido: %s
                Mensaje del validador: %s""".formatted(hallazgo.codigo(), titulo, hallazgo.severidad(), campo,
                hallazgo.ruta(), valor, hallazgo.mensaje());
    }
}
