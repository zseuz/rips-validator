package com.zseuz.rips.api;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.zseuz.rips.reglas.CodigoRegla;
import com.zseuz.rips.reglas.Severidad;
import com.zseuz.rips.rips.Rips;
import com.zseuz.rips.validacion.ResultadoValidacion;
import com.zseuz.rips.validacion.ResumenValidacion;
import com.zseuz.rips.validacion.ServicioValidacion;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1")
class ValidacionController {

    private final ServicioValidacion servicio;

    ValidacionController(ServicioValidacion servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Valida una transacción RIPS en JSON y guarda el resultado",
            description = "Responde 201 aunque el RIPS tenga errores: el estado y los hallazgos dicen si se puede enviar.")
    @PostMapping("/validaciones")
    ResponseEntity<ResultadoValidacion> validar(@RequestBody Rips rips) {
        ResultadoValidacion resultado = servicio.validar(rips);
        return ResponseEntity.created(URI.create("/api/v1/validaciones/" + resultado.id())).body(resultado);
    }

    @Operation(summary = "Historial de validaciones, de la más reciente a la más antigua")
    @GetMapping("/validaciones")
    PagedModel<ResumenValidacion> historial(
            @PageableDefault(size = 20, sort = "fechaValidacion", direction = Sort.Direction.DESC) Pageable pagina) {
        return new PagedModel<>(servicio.historial(pagina));
    }

    @Operation(summary = "Resultado completo de una validación, con sus hallazgos")
    @GetMapping("/validaciones/{id}")
    ResultadoValidacion buscar(@PathVariable long id) {
        return servicio.buscar(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la validación " + id));
    }

    @Operation(summary = "Catálogo de reglas que aplica el validador")
    @GetMapping("/reglas")
    List<DescripcionRegla> reglas() {
        return Arrays.stream(CodigoRegla.values())
                .map(r -> new DescripcionRegla(r.name(), r.severidad(), r.titulo()))
                .toList();
    }

    record DescripcionRegla(String codigo, Severidad severidad, String titulo) {
    }
}
