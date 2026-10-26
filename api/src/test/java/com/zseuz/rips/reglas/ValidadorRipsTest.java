package com.zseuz.rips.reglas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.zseuz.rips.Ejemplos;
import com.zseuz.rips.referencia.TablasReferencia;
import com.zseuz.rips.rips.Rips;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Cada prueba parte del RIPS válido de {@code ejemplos/rips-valido.json}, le cambia un dato y
 * comprueba que aparezca exactamente el hallazgo esperado.
 */
class ValidadorRipsTest {

    private static final JsonMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /** Las tablas reales de SISPRO (catalogos/*.csv); se leen una vez para toda la clase. */
    private static final TablasReferencia TABLAS = new TablasReferencia();

    private final ValidadorRips validador = new ValidadorRips(List.of(
            new ReglasTransaccion(),
            new ReglasUsuario(Ejemplos.RELOJ),
            new ReglasServicios(Ejemplos.RELOJ),
            new ReglasCoherenciaClinica(TABLAS)), TABLAS);

    @Test
    void elEjemploValidoNoTieneHallazgos() {
        assertThat(validar(rips -> { })).isEmpty();
    }

    @Test
    void transaccionSinUsuarios() {
        assertThat(validar(rips -> rips.putArray("usuarios")))
                .extracting(Hallazgo::codigo, Hallazgo::ruta)
                .containsExactly(tuple("RV010", "usuarios"));
    }

    @Test
    void campoObligatorioVacio() {
        assertThat(validar(rips -> rips.putNull("numFactura")))
                .extracting(Hallazgo::codigo, Hallazgo::ruta)
                .containsExactly(tuple("RV001", "numFactura"));
    }

    @Test
    void notaRipsSinFacturaNoExigeNumeroDeFactura() {
        assertThat(validar(rips -> rips.put("tipoNota", "RS").putNull("numFactura"))).isEmpty();
    }

    @Test
    void usuarioRepetido() {
        List<Hallazgo> hallazgos = validar(rips -> {
            ArrayNode usuarios = (ArrayNode) rips.get("usuarios");
            ObjectNode copia = (ObjectNode) usuarios.get(0).deepCopy();
            usuarios.add(copia.put("consecutivo", 3));
        });
        assertThat(hallazgos)
                .extracting(Hallazgo::codigo, Hallazgo::ruta)
                .containsExactly(tuple("RV011", "usuarios[2]"));
    }

    @Test
    void consecutivoFueraDeSecuencia() {
        assertThat(validar(rips -> consulta(rips, 0, 1).put("consecutivo", 5)))
                .extracting(Hallazgo::codigo, Hallazgo::ruta, Hallazgo::valor)
                .containsExactly(tuple("RV012", "usuarios[0].servicios.consultas[1].consecutivo", "5"));
    }

    @Test
    void codigoFueraDeCatalogo() {
        assertThat(validar(rips -> usuario(rips, 0).put("codSexo", "X")))
                .extracting(Hallazgo::codigo, Hallazgo::ruta)
                .containsExactly(tuple("RV003", "usuarios[0].codSexo"));
    }

    @Test
    void cupsQueNoExiste() {
        assertThat(validar(rips -> consulta(rips, 0, 0).put("codConsulta", "89020")))
                .extracting(Hallazgo::codigo, Hallazgo::ruta)
                .containsExactly(tuple("RV003", "usuarios[0].servicios.consultas[0].codConsulta"));
    }

    @Test
    void cupsDeshabilitadoEnSispro() {
        assertThat(validar(rips -> procedimiento(rips, 0, 0).put("codProcedimiento", "010100")))
                .extracting(Hallazgo::codigo, Hallazgo::mensaje)
                .containsExactly(tuple("RV005", "010100 (PUNCION CISTERNAL SOD) está deshabilitado en la tabla CUPSRips de SISPRO"));
    }

    @Test
    void cupsDeConsultaReportadoComoProcedimiento() {
        assertThat(validar(rips -> procedimiento(rips, 0, 0).put("codProcedimiento", "890201")))
                .extracting(Hallazgo::codigo, Hallazgo::ruta)
                .containsExactly(tuple("RV006", "usuarios[0].servicios.procedimientos[0].codProcedimiento"));
    }

    @Test
    void diagnosticoQueNoExisteEnLaCie10() {
        assertThat(validar(rips -> consulta(rips, 0, 0).put("codDiagnosticoPrincipal", "J999")))
                .extracting(Hallazgo::codigo, Hallazgo::ruta)
                .containsExactly(tuple("RV003", "usuarios[0].servicios.consultas[0].codDiagnosticoPrincipal"));
    }

    @Test
    void municipioQueNoExisteEnDivipola() {
        assertThat(validar(rips -> usuario(rips, 0).put("codMunicipioResidencia", "11002")))
                .extracting(Hallazgo::codigo, Hallazgo::ruta)
                .containsExactly(tuple("RV003", "usuarios[0].codMunicipioResidencia"));
    }

    @Test
    void campoOpcionalSeRevisaSiVieneDiligenciado() {
        assertThat(validar(rips -> consulta(rips, 0, 0).put("finalidadTecnologiaSalud", "99")))
                .extracting(Hallazgo::codigo, Hallazgo::ruta)
                .containsExactly(tuple("RV003", "usuarios[0].servicios.consultas[0].finalidadTecnologiaSalud"));
    }

    @Test
    void diagnosticoFueraDelRangoDeEdad() {
        // P070 (peso extremadamente bajo al nacer) solo aplica a menores de 1 año; la usuaria tiene 38.
        assertThat(validar(rips -> consulta(rips, 0, 0).put("codDiagnosticoPrincipal", "P070")))
                .extracting(Hallazgo::codigo, Hallazgo::severidad)
                .containsExactly(tuple("RV032", Severidad.ADVERTENCIA));
    }

    @Test
    void fechaDeNacimientoFutura() {
        assertThat(validar(rips -> usuario(rips, 1).put("fechaNacimiento", "2027-01-01")))
                .extracting(Hallazgo::codigo, Hallazgo::ruta)
                .containsExactly(tuple("RV004", "usuarios[1].fechaNacimiento"));
    }

    @Test
    void fechaQueNoExiste() {
        assertThat(validar(rips -> consulta(rips, 0, 0).put("fechaInicioAtencion", "2026-02-30 08:00")))
                .extracting(Hallazgo::codigo)
                .containsExactly("RV004");
    }

    @Test
    void atencionAnteriorAlNacimiento() {
        assertThat(validar(rips -> consulta(rips, 1, 0).put("fechaInicioAtencion", "2014-01-10 09:00")))
                .extracting(Hallazgo::codigo, Hallazgo::ruta)
                .containsExactly(tuple("RV021", "usuarios[1].servicios.consultas[0].fechaInicioAtencion"));
    }

    @Test
    void cedulaEnUnMenorEsAdvertencia() {
        assertThat(validar(rips -> usuario(rips, 1).put("tipoDocumentoIdentificacion", "CC")))
                .extracting(Hallazgo::codigo, Hallazgo::severidad)
                .containsExactly(tuple("RV020", Severidad.ADVERTENCIA));
    }

    @Test
    void municipioNoSeExigeAResidentesEnElExterior() {
        assertThat(validar(rips -> usuario(rips, 0).put("codPaisResidencia", "862").putNull("codMunicipioResidencia")))
                .isEmpty();
    }

    @Test
    void diagnosticoDeEmbarazoEnHombre() {
        assertThat(validar(rips -> consulta(rips, 1, 0).put("codDiagnosticoPrincipal", "O800")))
                .extracting(Hallazgo::codigo, Hallazgo::ruta)
                .containsExactly(tuple("RV030", "usuarios[1].servicios.consultas[0].codDiagnosticoPrincipal"));
    }

    @Test
    void diagnosticoDeProstataEnMujer() {
        assertThat(validar(rips -> consulta(rips, 0, 1).put("codDiagnosticoRelacionado2", "N40X")))
                .extracting(Hallazgo::codigo, Hallazgo::ruta)
                .containsExactly(tuple("RV030", "usuarios[0].servicios.consultas[1].codDiagnosticoRelacionado2"));
    }

    @Test
    void diagnosticoRelacionadoRepetido() {
        assertThat(validar(rips -> consulta(rips, 0, 0).put("codDiagnosticoRelacionado1", "J069")))
                .extracting(Hallazgo::codigo)
                .containsExactly("RV031");
    }

    @Test
    void pagoModeradorMayorQueElServicio() {
        assertThat(validar(rips -> consulta(rips, 1, 0).put("valorPagoModerador", 50000)))
                .extracting(Hallazgo::codigo, Hallazgo::severidad)
                .containsExactly(tuple("RV041", Severidad.ERROR));
    }

    @Test
    void valorNegativoYValorCero() {
        List<Hallazgo> hallazgos = validar(rips -> {
            consulta(rips, 0, 0).put("vrServicio", -1);
            consulta(rips, 0, 1).put("vrServicio", 0);
        });
        assertThat(hallazgos).extracting(Hallazgo::codigo).containsExactly("RV040", "RV042");
    }

    @Test
    void elEjemploConErroresReportaCadaProblema() {
        List<Hallazgo> hallazgos = validador.validar(leer(Ejemplos.leer("rips-con-errores.json")));
        assertThat(hallazgos).extracting(Hallazgo::codigo)
                .contains("RV002", "RV003", "RV004", "RV011", "RV012", "RV013", "RV030", "RV031", "RV040", "RV041", "RV001");
    }

    private List<Hallazgo> validar(Consumer<ObjectNode> cambio) {
        ObjectNode rips = (ObjectNode) JSON.readTree(Ejemplos.leer("rips-valido.json"));
        cambio.accept(rips);
        return validador.validar(JSON.treeToValue(rips, Rips.class));
    }

    private static Rips leer(String json) {
        return JSON.readValue(json, Rips.class);
    }

    private static ObjectNode usuario(ObjectNode rips, int indice) {
        return (ObjectNode) rips.get("usuarios").get(indice);
    }

    private static ObjectNode consulta(ObjectNode rips, int usuario, int indice) {
        return (ObjectNode) usuario(rips, usuario).get("servicios").get("consultas").get(indice);
    }

    private static ObjectNode procedimiento(ObjectNode rips, int usuario, int indice) {
        return (ObjectNode) usuario(rips, usuario).get("servicios").get("procedimientos").get(indice);
    }
}
