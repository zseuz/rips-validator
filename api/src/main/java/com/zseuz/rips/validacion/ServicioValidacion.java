package com.zseuz.rips.validacion;

import java.time.Clock;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zseuz.rips.factura.FacturaElectronica;
import com.zseuz.rips.reglas.ValidadorRips;
import com.zseuz.rips.rips.Rips;

@Service
public class ServicioValidacion {

    private final ValidadorRips validador;
    private final ValidacionRepository repositorio;
    private final Clock reloj;

    ServicioValidacion(ValidadorRips validador, ValidacionRepository repositorio, Clock reloj) {
        this.validador = validador;
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Transactional
    public ResultadoValidacion validar(Rips rips) {
        return validar(rips, null);
    }

    /** Igual que {@link #validar(Rips)}, cruzando además con la factura electrónica si viene. */
    @Transactional
    public ResultadoValidacion validar(Rips rips, FacturaElectronica factura) {
        Validacion validacion = new Validacion(rips, validador.validar(rips, factura), reloj.instant());
        return ResultadoValidacion.de(repositorio.save(validacion));
    }

    @Transactional(readOnly = true)
    public Optional<ResultadoValidacion> buscar(long id) {
        return repositorio.findById(id).map(ResultadoValidacion::de);
    }

    @Transactional(readOnly = true)
    public Page<ResumenValidacion> historial(Pageable pagina) {
        return repositorio.findAll(pagina).map(ResumenValidacion::de);
    }
}
