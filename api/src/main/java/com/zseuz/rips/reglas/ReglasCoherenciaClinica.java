package com.zseuz.rips.reglas;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.zseuz.rips.rips.Rips;
import com.zseuz.rips.rips.ServicioRips;
import com.zseuz.rips.rips.Usuario;

/**
 * Cruza los diagnósticos con los datos del usuario. Son los errores que más glosas generan porque
 * cada campo, por separado, tiene un formato válido.
 */
@Component
@Order(4)
class ReglasCoherenciaClinica implements ReglaRips {

    @Override
    public void validar(Rips rips, Hallazgos hallazgos) {
        List<Usuario> usuarios = rips.usuarios();
        for (int u = 0; u < usuarios.size(); u++) {
            Usuario usuario = usuarios.get(u);
            String sexo = usuario.codSexo();
            if (!"H".equals(sexo) && !"M".equals(sexo)) {
                continue;
            }
            String base = "usuarios[" + u + "].servicios.";
            revisar(usuario.servicios().consultas(), base + "consultas", sexo, hallazgos);
            revisar(usuario.servicios().procedimientos(), base + "procedimientos", sexo, hallazgos);
        }
    }

    private static void revisar(List<? extends ServicioRips> servicios, String rutaLista, String sexo, Hallazgos hallazgos) {
        for (int i = 0; i < servicios.size(); i++) {
            ServicioRips servicio = servicios.get(i);
            List<ServicioRips.Campo> diagnosticos = new ArrayList<>();
            diagnosticos.add(new ServicioRips.Campo("codDiagnosticoPrincipal", servicio.codDiagnosticoPrincipal()));
            diagnosticos.addAll(servicio.diagnosticosRelacionados());
            for (ServicioRips.Campo diagnostico : diagnosticos) {
                String codigo = diagnostico.valor();
                if (codigo == null || codigo.length() < 3) {
                    continue;
                }
                String ruta = rutaLista + "[" + i + "]." + diagnostico.nombre();
                if ("H".equals(sexo) && codigo.startsWith(Catalogos.PREFIJO_SOLO_MUJER)) {
                    hallazgos.agregar(CodigoRegla.RV030, ruta,
                            "Los diagnósticos del capítulo O (embarazo, parto y puerperio) no aplican a usuarios de sexo H", codigo);
                } else if ("M".equals(sexo) && Catalogos.CATEGORIAS_SOLO_HOMBRE.contains(codigo.substring(0, 3))) {
                    hallazgos.agregar(CodigoRegla.RV030, ruta,
                            "Los diagnósticos de próstata no aplican a usuarios de sexo M", codigo);
                }
            }
        }
    }
}
