package com.zseuz.rips.reglas;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.zseuz.rips.rips.Consulta;
import com.zseuz.rips.rips.Rips;
import com.zseuz.rips.rips.ServicioRips;
import com.zseuz.rips.rips.Usuario;

/** Campos comunes de consultas y procedimientos: prestador, fechas, códigos, diagnósticos y valores. */
@Component
@Order(3)
class ReglasServicios implements ReglaRips {

    private final Clock reloj;

    ReglasServicios(Clock reloj) {
        this.reloj = reloj;
    }

    @Override
    public void validar(Rips rips, Hallazgos hallazgos) {
        LocalDate hoy = LocalDate.now(reloj);
        List<Usuario> usuarios = rips.usuarios();
        for (int u = 0; u < usuarios.size(); u++) {
            Usuario usuario = usuarios.get(u);
            // Una fecha de nacimiento futura ya la reporta ReglasUsuario; aquí no se compara contra ella.
            Optional<LocalDate> nacimiento = Fechas.leerFecha(usuario.fechaNacimiento()).filter(f -> !f.isAfter(hoy));
            String base = "usuarios[" + u + "].servicios.";
            validarLista(usuario.servicios().consultas(), base + "consultas", nacimiento, hoy, hallazgos);
            validarLista(usuario.servicios().procedimientos(), base + "procedimientos", nacimiento, hoy, hallazgos);
        }
    }

    private void validarLista(List<? extends ServicioRips> servicios, String rutaLista, Optional<LocalDate> nacimiento,
            LocalDate hoy, Hallazgos hallazgos) {
        for (int i = 0; i < servicios.size(); i++) {
            ServicioRips servicio = servicios.get(i);
            String ruta = rutaLista + "[" + i + "].";
            validar(servicio, ruta, nacimiento, hoy, hallazgos);
            if (servicio.consecutivo() == null || servicio.consecutivo() != i + 1) {
                hallazgos.agregar(CodigoRegla.RV012, ruta + "consecutivo", "Se esperaba " + (i + 1), servicio.consecutivo());
            }
        }
    }

    private void validar(ServicioRips servicio, String ruta, Optional<LocalDate> nacimiento, LocalDate hoy, Hallazgos hallazgos) {
        if (hallazgos.obligatorio(ruta + "codPrestador", servicio.codPrestador())) {
            hallazgos.formato(ruta + "codPrestador", servicio.codPrestador(), Catalogos.CODIGO_PRESTADOR,
                    "el código de habilitación del REPS (12 dígitos)");
        }

        if (hallazgos.obligatorio(ruta + "fechaInicioAtencion", servicio.fechaInicioAtencion())) {
            hallazgos.fechaHora(ruta + "fechaInicioAtencion", servicio.fechaInicioAtencion(), hoy)
                    .filter(atencion -> nacimiento.isPresent() && atencion.isBefore(nacimiento.get()))
                    .ifPresent(atencion -> hallazgos.agregar(CodigoRegla.RV021, ruta + "fechaInicioAtencion",
                            "El usuario nació el " + nacimiento.get(), servicio.fechaInicioAtencion()));
        }

        String campoCups = ruta + servicio.campoCodigoTecnologia();
        if (hallazgos.obligatorio(campoCups, servicio.codigoTecnologia())) {
            hallazgos.formato(campoCups, servicio.codigoTecnologia(), Catalogos.CUPS, "un código CUPS de 6 caracteres");
        }

        validarDiagnosticos(servicio, ruta, hallazgos);
        if (servicio instanceof Consulta consulta
                && hallazgos.obligatorio(ruta + "tipoDiagnosticoPrincipal", consulta.tipoDiagnosticoPrincipal())) {
            hallazgos.catalogo(ruta + "tipoDiagnosticoPrincipal", consulta.tipoDiagnosticoPrincipal(), Catalogos.TIPOS_DIAGNOSTICO_PRINCIPAL);
        }

        if (hallazgos.obligatorio(ruta + "tipoDocumentoIdentificacion", servicio.tipoDocumentoIdentificacion())) {
            hallazgos.catalogo(ruta + "tipoDocumentoIdentificacion", servicio.tipoDocumentoIdentificacion(), Catalogos.TIPOS_DOCUMENTO);
        }
        if (hallazgos.obligatorio(ruta + "numDocumentoIdentificacion", servicio.numDocumentoIdentificacion())) {
            hallazgos.formato(ruta + "numDocumentoIdentificacion", servicio.numDocumentoIdentificacion(), Catalogos.NUMERO_DOCUMENTO,
                    "hasta 20 letras o dígitos, sin puntos ni espacios");
        }
        if (hallazgos.obligatorio(ruta + "conceptoRecaudo", servicio.conceptoRecaudo())) {
            hallazgos.catalogo(ruta + "conceptoRecaudo", servicio.conceptoRecaudo(), Catalogos.CONCEPTOS_RECAUDO);
        }

        validarValores(servicio, ruta, hallazgos);
    }

    private static void validarDiagnosticos(ServicioRips servicio, String ruta, Hallazgos hallazgos) {
        String principal = servicio.codDiagnosticoPrincipal();
        if (hallazgos.obligatorio(ruta + "codDiagnosticoPrincipal", principal)) {
            hallazgos.formato(ruta + "codDiagnosticoPrincipal", principal, Catalogos.CIE10, "un código CIE-10 (p. ej. J069)");
        }
        for (ServicioRips.Campo relacionado : servicio.diagnosticosRelacionados()) {
            hallazgos.formato(ruta + relacionado.nombre(), relacionado.valor(), Catalogos.CIE10, "un código CIE-10 (p. ej. J069)");
            if (principal != null && principal.equals(relacionado.valor())) {
                hallazgos.agregar(CodigoRegla.RV031, ruta + relacionado.nombre(),
                        "Repite el diagnóstico principal; déjelo vacío o reporte otro diagnóstico", relacionado.valor());
            }
        }
    }

    private static void validarValores(ServicioRips servicio, String ruta, Hallazgos hallazgos) {
        BigDecimal valor = servicio.vrServicio();
        if (hallazgos.obligatorio(ruta + "vrServicio", valor)) {
            if (valor.signum() < 0) {
                hallazgos.agregar(CodigoRegla.RV040, ruta + "vrServicio", "El valor no puede ser negativo", valor);
            } else if (valor.signum() == 0) {
                hallazgos.agregar(CodigoRegla.RV042, ruta + "vrServicio",
                        "Verifique si el servicio es realmente gratuito o si falta el valor", valor);
            }
        }
        BigDecimal moderador = servicio.valorPagoModerador();
        if (moderador != null) {
            if (moderador.signum() < 0) {
                hallazgos.agregar(CodigoRegla.RV040, ruta + "valorPagoModerador", "El valor no puede ser negativo", moderador);
            } else if (valor != null && valor.signum() >= 0 && moderador.compareTo(valor) > 0) {
                hallazgos.agregar(CodigoRegla.RV041, ruta + "valorPagoModerador",
                        "El pago moderador supera el valor del servicio (" + valor.toPlainString() + ")", moderador);
            }
        }
    }
}
