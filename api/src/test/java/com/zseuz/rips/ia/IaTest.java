package com.zseuz.rips.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.zseuz.rips.Ejemplos;
import com.zseuz.rips.reglas.Hallazgo;
import com.zseuz.rips.reglas.Severidad;

/** Sin clave (como en CI): la API arranca, avisa que la IA no está disponible y no llama a nadie. */
@SpringBootTest(properties = "rips.ia.api-key=")
@AutoConfigureMockMvc
class IaTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void sinClaveLaIaNoEstaDisponible() throws Exception {
        mvc.perform(get("/api/v1/ia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false));
    }

    @Test
    void sinClaveExplicarRespondeServicioNoDisponible() throws Exception {
        String ubicacion = mvc.perform(post("/api/v1/validaciones").contentType(MediaType.APPLICATION_JSON)
                        .content(Ejemplos.leer("rips-con-errores.json")))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(post(ubicacion + "/hallazgos/0/explicacion"))
                .andExpect(status().isServiceUnavailable());
        mvc.perform(post(ubicacion + "/hallazgos/999/explicacion"))
                .andExpect(status().isNotFound());
    }

    @Test
    void noEnviaDatosPersonalesAlModelo() {
        String texto = ExplicadorHallazgos.describir(new Hallazgo("RV011", Severidad.ERROR, "usuarios[2]",
                "El usuario ya aparece en usuarios[0]", "CC 1023456789"));
        assertThat(texto).doesNotContain("1023456789").contains("(omitido: dato personal)").contains("Usuario repetido");

        assertThat(ExplicadorHallazgos.describir(new Hallazgo("RV030", Severidad.ERROR,
                "usuarios[0].servicios.consultas[0].codDiagnosticoPrincipal", "O800 solo aplica a mujeres", "O800")))
                .contains("Valor recibido: O800").contains("Campo: codDiagnosticoPrincipal");
    }
}
