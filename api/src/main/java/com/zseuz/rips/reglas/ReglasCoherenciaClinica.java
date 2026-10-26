package com.zseuz.rips.reglas;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.zseuz.rips.referencia.CodigoReferencia;
import com.zseuz.rips.referencia.Tablas;
import com.zseuz.rips.referencia.TablasReferencia;
import com.zseuz.rips.rips.Atencion;
import com.zseuz.rips.rips.Rips;
import com.zseuz.rips.rips.Usuario;

/**
 * Cruza cada diagnóstico con el sexo y la edad del usuario según la tabla CIE-10 de SISPRO
 * (columnas AplicaASexo, EdadMinima y EdadMaxima). Son los errores que más glosas generan porque
 * cada campo, por separado, es válido.
 */
@Component
@Order(4)
class ReglasCoherenciaClinica implements ReglaRips {

    private final TablasReferencia tablas;

    ReglasCoherenciaClinica(TablasReferencia tablas) {
        this.tablas = tablas;
    }

    @Override
    public void validar(Rips rips, Hallazgos hallazgos) {
        List<Usuario> usuarios = rips.usuarios();
        for (int u = 0; u < usuarios.size(); u++) {
            Usuario usuario = usuarios.get(u);
            Optional<LocalDate> nacimiento = Fechas.leerFecha(usuario.fechaNacimiento());
            for (Map.Entry<String, List<? extends Atencion>> lista : usuario.servicios().porTipo().entrySet()) {
                // Los diagnósticos de un recién nacido son del bebé, no de la usuaria que lo reporta.
                if (!lista.getKey().equals("recienNacidos")) {
                    revisar(lista.getValue(), "usuarios[" + u + "].servicios." + lista.getKey(), usuario.codSexo(), nacimiento, hallazgos);
                }
            }
        }
    }

    private void revisar(List<? extends Atencion> atenciones, String rutaLista, String sexo, Optional<LocalDate> nacimiento,
            Hallazgos hallazgos) {
        for (int i = 0; i < atenciones.size(); i++) {
            Atencion atencion = atenciones.get(i);
            // Edad en años cumplidos el día de la atención, que es como la mide la CIE-10 de SISPRO.
            Optional<Integer> edad = nacimiento.flatMap(n -> Fechas.leerFechaHora(atencion.fechaAtencion())
                    .map(fecha -> Period.between(n, fecha.toLocalDate()).getYears()))
                    .filter(cumplidos -> cumplidos >= 0);

            List<Atencion.Campo> diagnosticos = new ArrayList<>();
            diagnosticos.add(new Atencion.Campo("codDiagnosticoPrincipal", atencion.codDiagnosticoPrincipal()));
            diagnosticos.addAll(atencion.diagnosticosRelacionados());
            for (Atencion.Campo campo : diagnosticos) {
                if (campo.valor() == null) {
                    continue;
                }
                String ruta = rutaLista + "[" + i + "]." + campo.nombre();
                tablas.buscar(Tablas.CIE10, campo.valor()).ifPresent(diagnostico -> {
                    revisarSexo(diagnostico, sexo, ruta, hallazgos);
                    edad.ifPresent(cumplidos -> revisarEdad(diagnostico, cumplidos, ruta, hallazgos));
                });
            }
        }
    }

    private static void revisarSexo(CodigoReferencia diagnostico, String sexo, String ruta, Hallazgos hallazgos) {
        // Con sexo indeterminado (I) no se puede afirmar que el diagnóstico no aplique.
        if (diagnostico.sexo() != null && ("H".equals(sexo) || "M".equals(sexo)) && !diagnostico.sexo().equals(sexo)) {
            hallazgos.agregar(CodigoRegla.RV030, ruta,
                    diagnostico.etiqueta() + " solo aplica a " + ("H".equals(diagnostico.sexo()) ? "hombres" : "mujeres")
                            + " según la CIE-10 de SISPRO",
                    diagnostico.codigo());
        }
    }

    private static void revisarEdad(CodigoReferencia diagnostico, int edad, String ruta, Hallazgos hallazgos) {
        Integer minima = diagnostico.edadMinima();
        Integer maxima = diagnostico.edadMaxima();
        if ((minima != null && edad < minima) || (maxima != null && edad > maxima)) {
            hallazgos.agregar(CodigoRegla.RV032, ruta,
                    diagnostico.etiqueta() + " aplica de " + minima + " a " + maxima + " años; el usuario tenía " + edad
                            + " el día de la atención",
                    diagnostico.codigo());
        }
    }
}
