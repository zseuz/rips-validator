package com.zseuz.rips.reglas;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.zseuz.rips.referencia.Tablas;
import com.zseuz.rips.rips.Rips;
import com.zseuz.rips.rips.Usuario;

/** Datos de identificación y residencia de cada usuario. */
@Component
@Order(2)
class ReglasUsuario implements ReglaRips {

    private final Clock reloj;

    ReglasUsuario(Clock reloj) {
        this.reloj = reloj;
    }

    @Override
    public void validar(Rips rips, Hallazgos hallazgos) {
        LocalDate hoy = LocalDate.now(reloj);
        List<Usuario> usuarios = rips.usuarios();
        for (int i = 0; i < usuarios.size(); i++) {
            validar(usuarios.get(i), "usuarios[" + i + "].", hoy, hallazgos);
        }
    }

    private void validar(Usuario usuario, String ruta, LocalDate hoy, Hallazgos hallazgos) {
        if (hallazgos.obligatorio(ruta + "tipoDocumentoIdentificacion", usuario.tipoDocumentoIdentificacion())) {
            hallazgos.referencia(ruta + "tipoDocumentoIdentificacion", usuario.tipoDocumentoIdentificacion(), Tablas.TIPO_DOCUMENTO);
        }
        if (hallazgos.obligatorio(ruta + "numDocumentoIdentificacion", usuario.numDocumentoIdentificacion())) {
            hallazgos.formato(ruta + "numDocumentoIdentificacion", usuario.numDocumentoIdentificacion(), Catalogos.NUMERO_DOCUMENTO,
                    "hasta 20 letras o dígitos, sin puntos ni espacios");
        }
        if (hallazgos.obligatorio(ruta + "tipoUsuario", usuario.tipoUsuario())) {
            hallazgos.referencia(ruta + "tipoUsuario", usuario.tipoUsuario(), Tablas.TIPO_USUARIO);
        }
        if (hallazgos.obligatorio(ruta + "codSexo", usuario.codSexo())) {
            hallazgos.referencia(ruta + "codSexo", usuario.codSexo(), Tablas.SEXO);
        }
        if (hallazgos.obligatorio(ruta + "codPaisResidencia", usuario.codPaisResidencia())) {
            hallazgos.referencia(ruta + "codPaisResidencia", usuario.codPaisResidencia(), Tablas.PAIS);
        }
        // El municipio solo existe para quienes residen en Colombia.
        if (Catalogos.PAIS_COLOMBIA.equals(usuario.codPaisResidencia())
                && hallazgos.obligatorio(ruta + "codMunicipioResidencia", usuario.codMunicipioResidencia())) {
            hallazgos.referencia(ruta + "codMunicipioResidencia", usuario.codMunicipioResidencia(), Tablas.MUNICIPIO);
        }
        if (hallazgos.obligatorio(ruta + "codZonaTerritorialResidencia", usuario.codZonaTerritorialResidencia())) {
            hallazgos.referencia(ruta + "codZonaTerritorialResidencia", usuario.codZonaTerritorialResidencia(), Tablas.ZONA);
        }
        if (hallazgos.obligatorio(ruta + "incapacidad", usuario.incapacidad())) {
            hallazgos.catalogo(ruta + "incapacidad", usuario.incapacidad(), Catalogos.SI_NO);
        }
        hallazgos.referencia(ruta + "codPaisOrigen", usuario.codPaisOrigen(), Tablas.PAIS);

        if (hallazgos.obligatorio(ruta + "fechaNacimiento", usuario.fechaNacimiento())) {
            hallazgos.fecha(ruta + "fechaNacimiento", usuario.fechaNacimiento(), hoy)
                    .ifPresent(nacimiento -> validarDocumentoSegunEdad(usuario, ruta, Period.between(nacimiento, hoy).getYears(), hallazgos));
        }

        if (usuario.servicios().total() == 0) {
            hallazgos.agregar(CodigoRegla.RV013, ruta + "servicios",
                    "No se reportan consultas ni procedimientos para este usuario", null);
        }
    }

    /**
     * Registro civil hasta los 6 años, tarjeta de identidad de 7 a 17 y cédula desde los 18. La
     * edad se calcula a hoy, así que un usuario que cumplió años después de la atención puede dar
     * un falso positivo; por eso es advertencia y no error.
     */
    private static void validarDocumentoSegunEdad(Usuario usuario, String ruta, int edad, Hallazgos hallazgos) {
        String tipo = usuario.tipoDocumentoIdentificacion();
        boolean incoherente = switch (tipo == null ? "" : tipo) {
            case "RC" -> edad >= 7;
            case "TI" -> edad < 7 || edad >= 18;
            case "CC" -> edad < 18;
            default -> false;
        };
        if (incoherente) {
            hallazgos.agregar(CodigoRegla.RV020, ruta + "tipoDocumentoIdentificacion",
                    "Un usuario de " + edad + " años no suele identificarse con " + tipo, tipo);
        }
    }
}
