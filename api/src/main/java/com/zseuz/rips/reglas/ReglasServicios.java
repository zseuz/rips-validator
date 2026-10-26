package com.zseuz.rips.reglas;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.zseuz.rips.referencia.Tablas;
import com.zseuz.rips.rips.Consulta;
import com.zseuz.rips.rips.Procedimiento;
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

        validarCups(servicio, ruta, hallazgos);
        validarDiagnosticos(servicio, ruta, hallazgos);

        // Campos que se revisan contra SISPRO cuando vienen diligenciados.
        hallazgos.referencia(ruta + "modalidadGrupoServicioTecSal", servicio.modalidadGrupoServicioTecSal(), Tablas.MODALIDAD);
        hallazgos.referencia(ruta + "grupoServicios", servicio.grupoServicios(), Tablas.GRUPO_SERVICIOS);
        hallazgos.referencia(ruta + "finalidadTecnologiaSalud", servicio.finalidadTecnologiaSalud(), Tablas.FINALIDAD);
        if (servicio instanceof Consulta consulta) {
            hallazgos.referencia(ruta + "causaMotivoAtencion", consulta.causaMotivoAtencion(), Tablas.CAUSA_EXTERNA);
            if (hallazgos.obligatorio(ruta + "tipoDiagnosticoPrincipal", consulta.tipoDiagnosticoPrincipal())) {
                hallazgos.referencia(ruta + "tipoDiagnosticoPrincipal", consulta.tipoDiagnosticoPrincipal(), Tablas.TIPO_DIAGNOSTICO);
            }
        } else if (servicio instanceof Procedimiento procedimiento) {
            hallazgos.referencia(ruta + "viaIngresoServicioSalud", procedimiento.viaIngresoServicioSalud(), Tablas.VIA_INGRESO);
        }

        if (hallazgos.obligatorio(ruta + "tipoDocumentoIdentificacion", servicio.tipoDocumentoIdentificacion())) {
            hallazgos.referencia(ruta + "tipoDocumentoIdentificacion", servicio.tipoDocumentoIdentificacion(), Tablas.TIPO_DOCUMENTO);
        }
        if (hallazgos.obligatorio(ruta + "numDocumentoIdentificacion", servicio.numDocumentoIdentificacion())) {
            hallazgos.formato(ruta + "numDocumentoIdentificacion", servicio.numDocumentoIdentificacion(), Catalogos.NUMERO_DOCUMENTO,
                    "hasta 20 letras o dígitos, sin puntos ni espacios");
        }
        if (hallazgos.obligatorio(ruta + "conceptoRecaudo", servicio.conceptoRecaudo())) {
            hallazgos.referencia(ruta + "conceptoRecaudo", servicio.conceptoRecaudo(), Tablas.CONCEPTO_RECAUDO);
        }

        validarValores(servicio, ruta, hallazgos);
    }

    /** El CUPS debe existir y, según la tabla CUPSRips, estar marcado para consultas (AC) o procedimientos (AP). */
    private static void validarCups(ServicioRips servicio, String ruta, Hallazgos hallazgos) {
        String campo = ruta + servicio.campoCodigoTecnologia();
        String esperado = servicio instanceof Consulta ? "AC" : "AP";
        if (hallazgos.obligatorio(campo, servicio.codigoTecnologia())) {
            hallazgos.referencia(campo, servicio.codigoTecnologia(), Tablas.CUPS)
                    .filter(cups -> cups.uso() != null && !esperado.equals(cups.uso()))
                    .ifPresent(cups -> hallazgos.agregar(CodigoRegla.RV006, campo,
                            cups.etiqueta() + " se reporta como " + ("AC".equals(cups.uso()) ? "consulta" : "procedimiento")
                                    + ", no como " + ("AC".equals(esperado) ? "consulta" : "procedimiento"),
                            cups.codigo()));
        }
    }

    private static void validarDiagnosticos(ServicioRips servicio, String ruta, Hallazgos hallazgos) {
        String principal = servicio.codDiagnosticoPrincipal();
        if (hallazgos.obligatorio(ruta + "codDiagnosticoPrincipal", principal)) {
            hallazgos.referencia(ruta + "codDiagnosticoPrincipal", principal, Tablas.CIE10);
        }
        for (ServicioRips.Campo relacionado : servicio.diagnosticosRelacionados()) {
            hallazgos.referencia(ruta + relacionado.nombre(), relacionado.valor(), Tablas.CIE10);
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
