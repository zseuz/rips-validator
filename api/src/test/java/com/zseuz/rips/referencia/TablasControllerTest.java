package com.zseuz.rips.referencia;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Las búsquedas de este controlador leen la copia en base de datos que deja SincronizadorTablas. */
@SpringBootTest
@AutoConfigureMockMvc
class TablasControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void resumenDeTablasCargadas() throws Exception {
        mvc.perform(get("/api/v1/tablas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tabla == 'CIE10')].registros").value(12634))
                .andExpect(jsonPath("$[?(@.tabla == 'CUPSRips')].registros").value(13640));
    }

    @Test
    void buscaUnDiagnosticoEnLaBaseDeDatos() throws Exception {
        mvc.perform(get("/api/v1/tablas/CIE10/O800"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("PARTO UNICO ESPONTANEO, PRESENTACION CEFALICA DE VERTICE"))
                .andExpect(jsonPath("$.sexo").value("M"))
                .andExpect(jsonPath("$.edadMinima").value(10))
                .andExpect(jsonPath("$.edadMaxima").value(54));
    }

    @Test
    void codigoQueNoExiste() throws Exception {
        mvc.perform(get("/api/v1/tablas/CIE10/J999"))
                .andExpect(status().isNotFound());
    }
}
