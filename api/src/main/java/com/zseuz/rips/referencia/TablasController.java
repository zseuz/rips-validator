package com.zseuz.rips.referencia;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/tablas")
class TablasController {

    private final TablasReferencia tablas;
    private final RegistroReferenciaRepository repositorio;

    TablasController(TablasReferencia tablas, RegistroReferenciaRepository repositorio) {
        this.tablas = tablas;
        this.repositorio = repositorio;
    }

    @Operation(summary = "Tablas de referencia de SISPRO cargadas, con su tamaño y fecha de descarga")
    @GetMapping
    List<TablasReferencia.ResumenTabla> resumen() {
        return tablas.resumen();
    }

    @Operation(summary = "Busca un código en una tabla de referencia (p. ej. CIE10/O800 o CUPSRips/890201)")
    @GetMapping("/{tabla}/{codigo}")
    CodigoReferencia buscar(@PathVariable String tabla, @PathVariable String codigo) {
        return repositorio.findById(new RegistroReferencia.Clave(tabla, codigo))
                .map(RegistroReferencia::aCodigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, codigo + " no está en la tabla " + tabla));
    }
}
