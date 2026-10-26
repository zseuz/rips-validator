package com.zseuz.rips.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.zseuz.rips.Ejemplos;

@SpringBootTest
@AutoConfigureMockMvc
class ValidacionControllerTest {

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Ejemplos.RELOJ;
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void validaYGuardaUnRipsCorrecto() throws Exception {
        String ubicacion = mvc.perform(post("/api/v1/validaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Ejemplos.leer("rips-valido.json")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.estado").value("VALIDO"))
                .andExpect(jsonPath("$.numFactura").value("FEV1001"))
                .andExpect(jsonPath("$.totalUsuarios").value(2))
                .andExpect(jsonPath("$.totalServicios").value(7))
                .andExpect(jsonPath("$.hallazgos").isEmpty())
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(ubicacion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDO"));
    }

    @Test
    void rechazaUnRipsConErroresYExplicaDondeEstan() throws Exception {
        mvc.perform(post("/api/v1/validaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Ejemplos.leer("rips-con-errores.json")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.hallazgos[*].codigo", hasItems("RV030", "RV041", "RV011")))
                .andExpect(jsonPath("$.hallazgos[?(@.codigo == 'RV030')].ruta",
                        hasItem("usuarios[0].servicios.consultas[0].codDiagnosticoPrincipal")));
    }

    @Test
    void historialPaginado() throws Exception {
        mvc.perform(post("/api/v1/validaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(Ejemplos.leer("rips-valido.json")));

        mvc.perform(get("/api/v1/validaciones").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].numFactura").exists())
                .andExpect(jsonPath("$.content[0].hallazgos").doesNotExist())
                .andExpect(jsonPath("$.page.size").value(5));
    }

    @Test
    void validacionInexistente() throws Exception {
        mvc.perform(get("/api/v1/validaciones/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void jsonMalFormado() throws Exception {
        mvc.perform(post("/api/v1/validaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarios\": ["))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogoDeReglas() throws Exception {
        mvc.perform(get("/api/v1/reglas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("RV001"))
                .andExpect(jsonPath("$[0].severidad").value("ERROR"));
    }
}
