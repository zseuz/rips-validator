package com.zseuz.rips.referencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Copia en base de datos de un código de SISPRO. Permite cruzar en SQL o Power BI los hallazgos
 * con el nombre del diagnóstico o del procedimiento. La carga la hace {@link SincronizadorTablas}.
 */
@Entity
@Table(name = "item_referencia")
@IdClass(RegistroReferencia.Clave.class)
public class RegistroReferencia {

    @Id
    @Column(length = 40)
    private String tabla;

    @Id
    @Column(length = 20)
    private String codigo;

    @Column(length = 500)
    private String nombre;

    private boolean habilitado;

    @Column(length = 1)
    private String sexo;

    private Integer edadMinima;
    private Integer edadMaxima;

    @Column(length = 2)
    private String uso;

    protected RegistroReferencia() {
    }

    CodigoReferencia aCodigo() {
        return new CodigoReferencia(tabla, codigo, nombre, habilitado, sexo, edadMinima, edadMaxima, uso);
    }

    public record Clave(String tabla, String codigo) {
    }
}
