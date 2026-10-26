package com.zseuz.rips.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.zseuz.rips.Ejemplos;

@SpringBootTest(properties = { "rips.limites.validaciones-por-minuto=2", "rips.limites.tamano-maximo=1KB" })
@AutoConfigureMockMvc
class LimitesFilterTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void rechazaArchivosMasGrandesQueElLimite() throws Exception {
        mvc.perform(post("/api/v1/validaciones").with(ip("10.0.0.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Ejemplos.leer("rips-valido.json"))) // ~5 KB
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }

    @Test
    void limitaLasValidacionesPorMinutoPorIp() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/validaciones").with(ip("10.0.0.2")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/v1/validaciones").with(ip("10.0.0.2")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        // Otra IP no se ve afectada.
        mvc.perform(post("/api/v1/validaciones").with(ip("10.0.0.3")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor ip(String direccion) {
        return peticion -> {
            peticion.setRemoteAddr(direccion);
            return peticion;
        };
    }
}
