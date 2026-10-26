package com.zseuz.rips.referencia;

import org.springframework.data.jpa.repository.JpaRepository;

interface RegistroReferenciaRepository extends JpaRepository<RegistroReferencia, RegistroReferencia.Clave> {
}
