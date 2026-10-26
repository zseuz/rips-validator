package com.zseuz.rips.reglas;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.zseuz.rips.referencia.Tablas;
import com.zseuz.rips.referencia.TablasReferencia;
import com.zseuz.rips.rips.Atencion;
import com.zseuz.rips.rips.Cobrable;
import com.zseuz.rips.rips.ConEgreso;
import com.zseuz.rips.rips.Consulta;
import com.zseuz.rips.rips.Hospitalizacion;
import com.zseuz.rips.rips.Medicamento;
import com.zseuz.rips.rips.OtroServicio;
import com.zseuz.rips.rips.Procedimiento;
import com.zseuz.rips.rips.RecienNacido;
import com.zseuz.rips.rips.Rips;
import com.zseuz.rips.rips.ServicioRips;
import com.zseuz.rips.rips.Urgencia;
import com.zseuz.rips.rips.Usuario;

/**
 * Los siete tipos de servicio. Primero lo común a todos (prestador, fecha, diagnósticos,
 * consecutivo) y luego lo de cada familia: valores ({@link Cobrable}), egreso ({@link ConEgreso}),
 * CUPS ({@link ServicioRips}) y los campos propios de cada tipo.
 */
@Component
@Order(3)
class ReglasServicios implements ReglaRips {

    /** En la tabla CondicionyDestinoUsuarioEgreso de SISPRO, 02 es "paciente muerto". */
    private static final String EGRESO_MUERTO = "02";

    private final Clock reloj;
    private final TablasReferencia tablas;

    ReglasServicios(Clock reloj, TablasReferencia tablas) {
        this.reloj = reloj;
        this.tablas = tablas;
    }

    @Override
    public void validar(Rips rips, Hallazgos hallazgos) {
        LocalDate hoy = LocalDate.now(reloj);
        List<Usuario> usuarios = rips.usuarios();
        for (int u = 0; u < usuarios.size(); u++) {
            Usuario usuario = usuarios.get(u);
            // Una fecha de nacimiento futura ya la reporta ReglasUsuario; aquí no se compara contra ella.
            Optional<LocalDate> nacimiento = Fechas.leerFecha(usuario.fechaNacimiento()).filter(f -> !f.isAfter(hoy));
            for (Map.Entry<String, List<? extends Atencion>> lista : usuario.servicios().porTipo().entrySet()) {
                validarLista(lista.getValue(), "usuarios[" + u + "].servicios." + lista.getKey(), nacimiento, hoy, hallazgos);
            }
        }
    }

    private void validarLista(List<? extends Atencion> atenciones, String rutaLista, Optional<LocalDate> nacimiento,
            LocalDate hoy, Hallazgos hallazgos) {
        for (int i = 0; i < atenciones.size(); i++) {
            Atencion atencion = atenciones.get(i);
            String ruta = rutaLista + "[" + i + "].";
            validarComun(atencion, ruta, nacimiento, hoy, hallazgos);
            if (atencion instanceof Cobrable cobrable) {
                validarCobrable(cobrable, ruta, hallazgos);
            }
            if (atencion instanceof ConEgreso conEgreso) {
                validarEgreso(conEgreso, ruta, hoy, hallazgos);
            }
            switch (atencion) {
                case ServicioRips servicio -> validarServicioCups(servicio, ruta, hallazgos);
                case Urgencia urgencia ->
                    hallazgos.referencia(ruta + "causaMotivoAtencion", urgencia.causaMotivoAtencion(), Tablas.CAUSA_EXTERNA);
                case Hospitalizacion hospitalizacion -> {
                    hallazgos.referencia(ruta + "causaMotivoAtencion", hospitalizacion.causaMotivoAtencion(), Tablas.CAUSA_EXTERNA);
                    hallazgos.referencia(ruta + "viaIngresoServicioSalud", hospitalizacion.viaIngresoServicioSalud(), Tablas.VIA_INGRESO);
                }
                case RecienNacido recienNacido -> validarRecienNacido(recienNacido, ruta, hallazgos);
                case Medicamento medicamento -> validarMedicamento(medicamento, ruta, hallazgos);
                case OtroServicio otro -> validarOtroServicio(otro, ruta, hallazgos);
                default -> {
                }
            }
            if (atencion.consecutivo() == null || atencion.consecutivo() != i + 1) {
                hallazgos.agregar(CodigoRegla.RV012, ruta + "consecutivo", "Se esperaba " + (i + 1), atencion.consecutivo());
            }
        }
    }

    // --- Común a todos los tipos ----------------------------------------------------------------

    private void validarComun(Atencion atencion, String ruta, Optional<LocalDate> nacimiento, LocalDate hoy, Hallazgos hallazgos) {
        if (hallazgos.obligatorio(ruta + "codPrestador", atencion.codPrestador())) {
            validarPrestador(atencion.codPrestador(), ruta + "codPrestador", hallazgos);
        }

        String campoFecha = ruta + atencion.campoFechaAtencion();
        if (hallazgos.obligatorio(campoFecha, atencion.fechaAtencion())) {
            // El recién nacido nace después que la madre: la comparación con su nacimiento no aplica.
            hallazgos.fechaHora(campoFecha, atencion.fechaAtencion(), hoy)
                    .filter(fecha -> !(atencion instanceof RecienNacido))
                    .filter(fecha -> nacimiento.isPresent() && fecha.isBefore(nacimiento.get()))
                    .ifPresent(fecha -> hallazgos.agregar(CodigoRegla.RV021, campoFecha,
                            "El usuario nació el " + nacimiento.get(), atencion.fechaAtencion()));
        }

        validarDiagnosticos(atencion, ruta, hallazgos);
    }

    /**
     * El código debe existir en el REPS (IPSCodHabilitacion) o en la lista de entidades autorizadas
     * que no están en el REPS (IPSnoREPS), y estar habilitado.
     */
    private void validarPrestador(String codigo, String ruta, Hallazgos hallazgos) {
        if (!Catalogos.CODIGO_PRESTADOR.matcher(codigo).matches()) {
            hallazgos.agregar(CodigoRegla.RV002, ruta, "Se esperaba el código de habilitación del REPS (12 dígitos)", codigo);
            return;
        }
        if (!tablas.tiene(Tablas.REPS)) {
            return;
        }
        tablas.buscar(Tablas.REPS, codigo).or(() -> tablas.buscar(Tablas.NO_REPS, codigo)).ifPresentOrElse(
                prestador -> {
                    if (!prestador.habilitado()) {
                        hallazgos.agregar(CodigoRegla.RV005, ruta, prestador.etiqueta() + " está deshabilitado en el REPS", codigo);
                    }
                },
                () -> hallazgos.agregar(CodigoRegla.RV007, ruta,
                        "No está en el REPS (tabla IPSCodHabilitacion de SISPRO) ni entre las entidades autorizadas sin REPS", codigo));
    }

    private static void validarDiagnosticos(Atencion atencion, String ruta, Hallazgos hallazgos) {
        String principal = atencion.codDiagnosticoPrincipal();
        if (atencion.requiereDiagnostico() && hallazgos.obligatorio(ruta + "codDiagnosticoPrincipal", principal)) {
            hallazgos.referencia(ruta + "codDiagnosticoPrincipal", principal, Tablas.CIE10);
        }
        String principalEgreso = atencion.diagnosticosRelacionados().stream()
                .filter(c -> c.nombre().equals("codDiagnosticoPrincipalE")).map(Atencion.Campo::valor).findFirst().orElse(null);
        for (Atencion.Campo campo : atencion.diagnosticosRelacionados()) {
            hallazgos.referencia(ruta + campo.nombre(), campo.valor(), Tablas.CIE10);
            // Un relacionado de egreso (E1..E3) repite el principal de egreso; uno de ingreso, el principal.
            String comparado = campo.nombre().startsWith("codDiagnosticoRelacionadoE") ? principalEgreso
                    : campo.nombre().startsWith("codDiagnosticoRelacionado") ? principal : null;
            if (comparado != null && comparado.equals(campo.valor())) {
                hallazgos.agregar(CodigoRegla.RV031, ruta + campo.nombre(),
                        "Repite el diagnóstico principal; déjelo vacío o reporte otro diagnóstico", campo.valor());
            }
        }
    }

    // --- Por familia ----------------------------------------------------------------------------

    private static void validarCobrable(Cobrable servicio, String ruta, Hallazgos hallazgos) {
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

    private static void validarEgreso(ConEgreso atencion, String ruta, LocalDate hoy, Hallazgos hallazgos) {
        if (hallazgos.obligatorio(ruta + "fechaEgreso", atencion.fechaEgreso())
                && hallazgos.fechaHora(ruta + "fechaEgreso", atencion.fechaEgreso(), hoy).isPresent()) {
            Optional<LocalDateTime> ingreso = Fechas.leerFechaHora(atencion.fechaAtencion());
            Optional<LocalDateTime> egreso = Fechas.leerFechaHora(atencion.fechaEgreso());
            if (ingreso.isPresent() && egreso.isPresent() && egreso.get().isBefore(ingreso.get())) {
                hallazgos.agregar(CodigoRegla.RV022, ruta + "fechaEgreso",
                        "El ingreso fue el " + atencion.fechaAtencion(), atencion.fechaEgreso());
            }
        }
        hallazgos.referencia(ruta + "condicionDestinoUsuarioEgreso", atencion.condicionDestinoUsuarioEgreso(), Tablas.CONDICION_EGRESO);
        if (EGRESO_MUERTO.equals(atencion.condicionDestinoUsuarioEgreso())
                && (atencion.codDiagnosticoCausaMuerte() == null || atencion.codDiagnosticoCausaMuerte().isBlank())) {
            hallazgos.agregar(CodigoRegla.RV001, ruta + "codDiagnosticoCausaMuerte",
                    "Es obligatorio cuando el usuario egresa muerto (condición 02)", null);
        }
        hallazgos.referencia(ruta + "codDiagnosticoCausaMuerte", atencion.codDiagnosticoCausaMuerte(), Tablas.CIE10);
    }

    // --- Por tipo -------------------------------------------------------------------------------

    private static void validarServicioCups(ServicioRips servicio, String ruta, Hallazgos hallazgos) {
        validarCups(servicio, ruta, hallazgos);
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

    private static void validarRecienNacido(RecienNacido recienNacido, String ruta, Hallazgos hallazgos) {
        if (hallazgos.obligatorio(ruta + "tipoDocumentoIdentificacion", recienNacido.tipoDocumentoIdentificacion())) {
            hallazgos.referencia(ruta + "tipoDocumentoIdentificacion", recienNacido.tipoDocumentoIdentificacion(), Tablas.TIPO_DOCUMENTO);
        }
        positivo(ruta + "peso", recienNacido.peso(), hallazgos);
    }

    private static void validarMedicamento(Medicamento medicamento, String ruta, Hallazgos hallazgos) {
        if (hallazgos.obligatorio(ruta + "tipoMedicamento", medicamento.tipoMedicamento())) {
            hallazgos.referencia(ruta + "tipoMedicamento", medicamento.tipoMedicamento(), Tablas.TIPO_MEDICAMENTO);
        }
        hallazgos.obligatorio(ruta + "codTecnologiaSalud", medicamento.codTecnologiaSalud());
        if (medicamento.unidadMedida() != null) {
            hallazgos.referencia(ruta + "unidadMedida", medicamento.unidadMedida().toString(), Tablas.UNIDAD_MEDIDA);
        }
        hallazgos.referencia(ruta + "formaFarmaceutica", medicamento.formaFarmaceutica(), Tablas.FORMA_FARMACEUTICA);
        if (hallazgos.obligatorio(ruta + "cantidadMedicamento", medicamento.cantidadMedicamento())) {
            positivo(ruta + "cantidadMedicamento", medicamento.cantidadMedicamento(), hallazgos);
        }
    }

    private static void validarOtroServicio(OtroServicio otro, String ruta, Hallazgos hallazgos) {
        if (hallazgos.obligatorio(ruta + "tipoOS", otro.tipoOS())) {
            hallazgos.referencia(ruta + "tipoOS", otro.tipoOS(), Tablas.TIPO_OTRO_SERVICIO);
        }
        hallazgos.obligatorio(ruta + "codTecnologiaSalud", otro.codTecnologiaSalud());
        if (hallazgos.obligatorio(ruta + "cantidadOS", otro.cantidadOS())) {
            positivo(ruta + "cantidadOS", otro.cantidadOS(), hallazgos);
        }
    }

    private static void positivo(String ruta, BigDecimal valor, Hallazgos hallazgos) {
        if (valor != null && valor.signum() <= 0) {
            hallazgos.agregar(CodigoRegla.RV043, ruta, "Debe ser mayor que cero", valor);
        }
    }

    private static void validarValores(Cobrable servicio, String ruta, Hallazgos hallazgos) {
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
