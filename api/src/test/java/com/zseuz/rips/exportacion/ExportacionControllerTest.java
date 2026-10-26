package com.zseuz.rips.exportacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.zseuz.rips.Ejemplos;

@SpringBootTest
@AutoConfigureMockMvc
class ExportacionControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void hallazgosEnCsvConElNombreOficialDelCodigo() throws Exception {
        mvc.perform(post("/api/v1/validaciones").contentType(MediaType.APPLICATION_JSON).content(Ejemplos.leer("rips-con-errores.json")));

        String csv = texto(descargar(get("/api/v1/exportaciones/hallazgos.csv")));

        assertThat(csv).startsWith("IdValidacion,Orden,Regla,Severidad,Usuario,TipoServicio,Campo,Ruta,Mensaje,Valor,TablaReferencia,NombreReferencia");
        // Diagnóstico de embarazo en hombre: usuario 1, consulta, con el nombre que trae la CIE-10 de SISPRO.
        assertThat(csv).containsPattern(
                "RV030,ERROR,1,Consulta,codDiagnosticoPrincipal,usuarios\\[0]\\.servicios\\.consultas\\[0]\\.codDiagnosticoPrincipal,"
                        + "\"O800 \\(PARTO UNICO ESPONTANEO, PRESENTACION CEFALICA DE VERTICE\\) solo aplica a mujeres según la CIE-10 de SISPRO\","
                        + "O800,CIE10,\"PARTO UNICO ESPONTANEO, PRESENTACION CEFALICA DE VERTICE\"");
        // CUPS inexistente: se marca para que en Power BI se vea como error de digitación.
        assertThat(csv).contains(",89020,CUPSRips,(no existe en SISPRO)");
    }

    @Test
    void validacionesYReglasEnCsv() throws Exception {
        mvc.perform(post("/api/v1/validaciones").contentType(MediaType.APPLICATION_JSON).content(Ejemplos.leer("rips-valido.json")));

        assertThat(texto(descargar(get("/api/v1/exportaciones/validaciones.csv"))))
                .startsWith("IdValidacion,Factura,Obligado,Estado,FechaValidacion,Usuarios,Servicios,Errores,Advertencias")
                .containsPattern(",FEV1001,900123456,Válido,\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},2,4,0,0");
        assertThat(texto(descargar(get("/api/v1/exportaciones/reglas.csv"))))
                .contains("RV032,ADVERTENCIA,Diagnóstico fuera del rango de edad de la CIE-10,Coherencia clínica");
    }

    @Test
    void zipDePowerBiApuntaALaApiDesdeLaQueSeDescargo() throws Exception {
        MvcResult respuesta = descargar(get("/api/v1/exportaciones/power-bi.zip").header("Host", "rips.ejemplo.co:9090"));
        assertThat(respuesta.getResponse().getHeader("Content-Disposition")).isEqualTo("attachment; filename=\"RipsValidator-PowerBI.zip\"");

        Map<String, String> archivos = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(respuesta.getResponse().getContentAsByteArray()))) {
            for (ZipEntry entrada = zip.getNextEntry(); entrada != null; entrada = zip.getNextEntry()) {
                archivos.put(entrada.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertThat(archivos).containsKeys(
                "RipsValidator/RipsValidator.pbip",
                "RipsValidator/RipsValidator.Report/report.json",
                "RipsValidator/RipsValidator.SemanticModel/model.bim");
        assertThat(archivos).doesNotContainKey("RipsValidator/generar_pbip.py");
        assertThat(archivos.get("RipsValidator/RipsValidator.SemanticModel/model.bim"))
                .contains("\\\"http://rips.ejemplo.co:9090\\\" meta [IsParameterQuery=true")
                .doesNotContain("localhost:8080");
    }

    @Test
    void ubicacionSaleDeLaRuta() {
        assertThat(UbicacionHallazgo.de("usuarios[1].servicios.procedimientos[0].codProcedimiento"))
                .isEqualTo(new UbicacionHallazgo(2, "Procedimiento", "codProcedimiento", "CUPSRips"));
        assertThat(UbicacionHallazgo.de("usuarios[2]"))
                .isEqualTo(new UbicacionHallazgo(3, "Usuario", "usuarios", null));
        assertThat(UbicacionHallazgo.de("numFactura"))
                .isEqualTo(new UbicacionHallazgo(null, "Transacción", "numFactura", null));
    }

    /** Las exportaciones se escriben en streaming (respuesta asíncrona): hay que esperar a que terminen. */
    private MvcResult descargar(MockHttpServletRequestBuilder peticion) throws Exception {
        MvcResult iniciada = mvc.perform(peticion).andExpect(request().asyncStarted()).andReturn();
        return mvc.perform(asyncDispatch(iniciada)).andExpect(status().isOk()).andReturn();
    }

    private static String texto(MvcResult respuesta) throws Exception {
        return respuesta.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
