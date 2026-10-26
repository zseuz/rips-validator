package com.zseuz.rips.ia;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.zseuz.rips.reglas.Hallazgo;
import com.zseuz.rips.validacion.ServicioValidacion;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1")
class IaController {

    private final ExplicadorHallazgos explicador;
    private final ServicioValidacion validaciones;

    IaController(ExplicadorHallazgos explicador, ServicioValidacion validaciones) {
        this.explicador = explicador;
        this.validaciones = validaciones;
    }

    @Operation(summary = "Si la explicación con IA está disponible (hay GEMINI_API_KEY o ANTHROPIC_API_KEY) y con qué modelo")
    @GetMapping("/ia")
    EstadoIa estado() {
        return new EstadoIa(explicador.disponible(), explicador.modelo());
    }

    @Operation(summary = "Explica un hallazgo en lenguaje sencillo con IA (Gemini o Claude)",
            description = "posicion es el índice del hallazgo en la lista de la validación, desde 0. No se envían datos personales.")
    @PostMapping("/validaciones/{id}/hallazgos/{posicion}/explicacion")
    Explicacion explicar(@PathVariable long id, @PathVariable int posicion) {
        List<Hallazgo> hallazgos = validaciones.buscar(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la validación " + id))
                .hallazgos();
        if (posicion < 0 || posicion >= hallazgos.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La validación " + id + " no tiene el hallazgo " + posicion);
        }
        return explicador.explicar(id, posicion, hallazgos.get(posicion));
    }

    record EstadoIa(boolean disponible, String modelo) {
    }
}
