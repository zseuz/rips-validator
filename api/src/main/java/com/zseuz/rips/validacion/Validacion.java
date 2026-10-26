package com.zseuz.rips.validacion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.zseuz.rips.reglas.Hallazgo;
import com.zseuz.rips.reglas.Severidad;
import com.zseuz.rips.rips.Rips;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Resultado de validar una transacción. Se guarda solo el resumen y los hallazgos, nunca el RIPS:
 * el archivo trae datos personales y de salud que este servicio no necesita conservar.
 */
@Entity
@Table(indexes = @Index(name = "ix_validacion_fecha", columnList = "fechaValidacion"))
public class Validacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String numFactura;

    @Column(length = 20)
    private String numDocumentoIdObligado;

    @Enumerated(EnumType.STRING)
    @Column(length = 25, nullable = false)
    private EstadoValidacion estado;

    private int totalUsuarios;
    private int totalServicios;
    private long totalErrores;
    private long totalAdvertencias;

    @Column(nullable = false)
    private Instant fechaValidacion;

    @ElementCollection
    @CollectionTable(name = "hallazgo", joinColumns = @JoinColumn(name = "validacion_id"))
    @OrderColumn(name = "orden")
    private List<HallazgoValidacion> hallazgos = new ArrayList<>();

    protected Validacion() {
    }

    Validacion(Rips rips, List<Hallazgo> hallazgos, Instant fechaValidacion) {
        this.numFactura = recortar(rips.numFactura());
        this.numDocumentoIdObligado = recortar(rips.numDocumentoIdObligado());
        this.totalUsuarios = rips.usuarios().size();
        this.totalServicios = rips.usuarios().stream().mapToInt(u -> u.servicios().total()).sum();
        this.totalErrores = hallazgos.stream().filter(h -> h.severidad() == Severidad.ERROR).count();
        this.totalAdvertencias = hallazgos.size() - totalErrores;
        this.estado = EstadoValidacion.segun(totalErrores, totalAdvertencias);
        this.fechaValidacion = fechaValidacion;
        hallazgos.forEach(h -> this.hallazgos.add(new HallazgoValidacion(h)));
    }

    private static String recortar(String texto) {
        return texto != null && texto.length() > 20 ? texto.substring(0, 20) : texto;
    }

    public Long getId() {
        return id;
    }

    public String getNumFactura() {
        return numFactura;
    }

    public String getNumDocumentoIdObligado() {
        return numDocumentoIdObligado;
    }

    public EstadoValidacion getEstado() {
        return estado;
    }

    public int getTotalUsuarios() {
        return totalUsuarios;
    }

    public int getTotalServicios() {
        return totalServicios;
    }

    public long getTotalErrores() {
        return totalErrores;
    }

    public long getTotalAdvertencias() {
        return totalAdvertencias;
    }

    public Instant getFechaValidacion() {
        return fechaValidacion;
    }

    public List<Hallazgo> getHallazgos() {
        return hallazgos.stream().map(HallazgoValidacion::aHallazgo).toList();
    }
}
