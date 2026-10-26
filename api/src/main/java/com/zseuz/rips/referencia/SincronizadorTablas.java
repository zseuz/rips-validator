package com.zseuz.rips.referencia;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Al arrancar, reemplaza el contenido de {@code item_referencia} por las tablas empaquetadas.
 * Así la base siempre coincide con la versión de las tablas que usa el validador.
 */
@Component
class SincronizadorTablas implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SincronizadorTablas.class);
    private static final int LOTE = 1000;

    private final TablasReferencia tablas;
    private final JdbcTemplate jdbc;

    SincronizadorTablas(TablasReferencia tablas, JdbcTemplate jdbc) {
        this.tablas = tablas;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long inicio = System.currentTimeMillis();
        jdbc.update("delete from item_referencia");
        // JDBC por lotes y no JPA: son decenas de miles de filas y no se necesita el contexto de persistencia.
        List<Object[]> filas = new ArrayList<>(LOTE);
        int total = 0;
        for (Map<String, CodigoReferencia> tabla : tablas.todas()) {
            for (CodigoReferencia c : tabla.values()) {
                filas.add(new Object[] { c.tabla(), c.codigo(), c.nombre(), c.habilitado(), c.sexo(), c.edadMinima(), c.edadMaxima(), c.uso() });
                if (filas.size() == LOTE) {
                    total += insertar(filas);
                }
            }
        }
        total += insertar(filas);
        log.info("Tablas de referencia de SISPRO en base de datos: {} códigos en {} ms", total, System.currentTimeMillis() - inicio);
    }

    private int insertar(List<Object[]> filas) {
        if (filas.isEmpty()) {
            return 0;
        }
        jdbc.batchUpdate("insert into item_referencia (tabla, codigo, nombre, habilitado, sexo, edad_minima, edad_maxima, uso) "
                + "values (?, ?, ?, ?, ?, ?, ?, ?)", filas);
        int insertadas = filas.size();
        filas.clear();
        return insertadas;
    }
}
