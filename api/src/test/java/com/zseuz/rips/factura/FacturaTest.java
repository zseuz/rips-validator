package com.zseuz.rips.factura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.zseuz.rips.Ejemplos;

@SpringBootTest
@AutoConfigureMockMvc
class FacturaTest {

    private static final String FACTURA = Ejemplos.leer("factura-FEV1001.xml");

    @Autowired
    private MockMvc mvc;

    private final LectorFacturaXml lector = new LectorFacturaXml();

    @Test
    void leeLaFacturaUbl() {
        assertThat(lector.leer(bytes(FACTURA)))
                .isEqualTo(new FacturaElectronica("FEV1001", "900123456", LocalDate.of(2026, 10, 5), new BigDecimal("158600.00")));
    }

    @Test
    void leeLaFacturaDentroDelAttachedDocument() {
        String adjunto = """
                <AttachedDocument xmlns="urn:oasis:names:specification:ubl:schema:xsd:AttachedDocument-2"
                    xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
                    xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
                  <cac:Attachment><cac:ExternalReference><cbc:Description><![CDATA[%s]]></cbc:Description></cac:ExternalReference></cac:Attachment>
                </AttachedDocument>""".formatted(FACTURA.substring(FACTURA.indexOf("<Invoice")));
        assertThat(lector.leer(bytes(adjunto)).numero()).isEqualTo("FEV1001");
    }

    @Test
    void rechazaEntidadesExternas() {
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE x [<!ENTITY secreto SYSTEM "file:///etc/passwd">]>
                <Invoice><ID>&secreto;</ID></Invoice>""";
        assertThatThrownBy(() -> lector.leer(bytes(xxe))).isInstanceOf(LectorFacturaXml.FacturaIlegibleException.class);
    }

    @Test
    void elRipsValidoCuadraConSuFactura() throws Exception {
        mvc.perform(multipart("/api/v1/validaciones/con-factura")
                        .file(archivo("rips", "rips-valido.json", Ejemplos.leer("rips-valido.json")))
                        .file(archivo("factura", "factura.xml", FACTURA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("VALIDO"))
                .andExpect(jsonPath("$.hallazgos").isEmpty());
    }

    @Test
    void detectaDiferenciasConLaFactura() throws Exception {
        String otraFactura = FACTURA.replace(">FEV1001<", ">FEV2000<").replace(">900123456<", ">800111222<")
                .replace(">158600.00<", ">200000.00<").replace(">2026-10-05<", ">2026-09-30<");
        mvc.perform(multipart("/api/v1/validaciones/con-factura")
                        .file(archivo("rips", "rips-valido.json", Ejemplos.leer("rips-valido.json")))
                        .file(archivo("factura", "factura.xml", otraFactura)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.hallazgos[*].codigo").value(org.hamcrest.Matchers.containsInAnyOrder("RV050", "RV051", "RV052", "RV053")))
                .andExpect(jsonPath("$.hallazgos[?(@.codigo == 'RV053')].ruta").value("usuarios[1].servicios.consultas[0].fechaInicioAtencion"));
    }

    @Test
    void xmlIlegibleDaUnErrorClaro() throws Exception {
        mvc.perform(multipart("/api/v1/validaciones/con-factura")
                        .file(archivo("rips", "rips-valido.json", Ejemplos.leer("rips-valido.json")))
                        .file(archivo("factura", "factura.xml", "esto no es xml")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("El archivo no es un XML válido")));
    }

    private static MockMultipartFile archivo(String parte, String nombre, String contenido) {
        return new MockMultipartFile(parte, nombre, null, bytes(contenido));
    }

    private static byte[] bytes(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }
}
