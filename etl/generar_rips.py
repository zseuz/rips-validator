"""Genera RIPS sintéticos en JSON para probar el validador.

Ningún dato corresponde a personas reales: documentos, fechas y prestadores se inventan al azar.
Una parte de las facturas lleva errores inyectados a propósito (ver ERRORES) para que el
validador tenga algo que encontrar.

Uso:
    python generar_rips.py --facturas 50 --tasa-errores 0.3 --salida datos
"""

from __future__ import annotations

import argparse
import copy
import json
import random
from datetime import date, datetime, timedelta
from pathlib import Path

CUPS_CONSULTA = ["890201", "890301", "890701", "890202"]
CUPS_PROCEDIMIENTO = ["902210", "903841", "907106", "871121", "903895"]
CIE10_GENERALES = ["J069", "A09X", "I10X", "E119", "K297", "M545", "N390", "R51X", "Z000", "J00X"]
MUNICIPIOS = ["11001", "05001", "76001", "08001", "68001", "13001"]
PRESTADORES = ["110010000001", "050010000002", "760010000003", "080010000004"]


def documento_segun_edad(edad: int) -> str:
    if edad < 7:
        return "RC"
    if edad < 18:
        return "TI"
    return "CC"


def fecha_atencion(rng: random.Random, nacimiento: date, hoy: date) -> str:
    desde = max(nacimiento, hoy - timedelta(days=60))
    dia = desde + timedelta(days=rng.randint(0, (hoy - desde).days))
    return datetime(dia.year, dia.month, dia.day, rng.randint(7, 18), rng.choice([0, 15, 30, 45])).strftime("%Y-%m-%d %H:%M")


def servicio_base(rng: random.Random, nacimiento: date, hoy: date, diagnostico: str, consecutivo: int) -> dict:
    return {
        "codPrestador": rng.choice(PRESTADORES),
        "fechaInicioAtencion": fecha_atencion(rng, nacimiento, hoy),
        "numAutorizacion": None,
        "modalidadGrupoServicioTecSal": "01",
        "codServicio": 325,
        "finalidadTecnologiaSalud": "15",
        "codDiagnosticoPrincipal": diagnostico,
        "tipoDocumentoIdentificacion": "CC",
        "numDocumentoIdentificacion": str(rng.randint(10_000_000, 99_999_999)),
        "vrServicio": rng.choice([18500, 32000, 45000, 60000, 85000]),
        "conceptoRecaudo": "05",
        "valorPagoModerador": 0,
        "numFEVPagoModerador": None,
        "consecutivo": consecutivo,
    }


def generar_usuario(rng: random.Random, consecutivo: int, hoy: date) -> dict:
    edad = rng.randint(0, 85)
    nacimiento = hoy - timedelta(days=edad * 365 + rng.randint(1, 360))
    sexo = rng.choice(["H", "M"])

    consultas = []
    for i in range(rng.randint(1, 3)):
        diagnostico = "Z340" if sexo == "M" and 15 <= edad <= 45 and rng.random() < 0.2 else rng.choice(CIE10_GENERALES)
        consulta = servicio_base(rng, nacimiento, hoy, diagnostico, i + 1)
        consulta.update({
            "codConsulta": rng.choice(CUPS_CONSULTA),
            "grupoServicios": "01",
            "causaMotivoAtencion": "38",
            "codDiagnosticoRelacionado1": None,
            "codDiagnosticoRelacionado2": None,
            "codDiagnosticoRelacionado3": None,
            "tipoDiagnosticoPrincipal": rng.choice(["01", "02", "03"]),
        })
        consultas.append(consulta)

    procedimientos = []
    for i in range(rng.randint(0, 2)):
        procedimiento = servicio_base(rng, nacimiento, hoy, rng.choice(CIE10_GENERALES), i + 1)
        procedimiento.update({
            "idMIPRES": None,
            "codProcedimiento": rng.choice(CUPS_PROCEDIMIENTO),
            "viaIngresoServicioSalud": "02",
            "grupoServicios": "02",
            "codDiagnosticoRelacionado": None,
            "codComplicacion": None,
        })
        procedimientos.append(procedimiento)

    return {
        "tipoDocumentoIdentificacion": documento_segun_edad(edad),
        "numDocumentoIdentificacion": str(rng.randint(1_000_000_000, 1_199_999_999)),
        "tipoUsuario": rng.choice(["01", "02", "04"]),
        "fechaNacimiento": nacimiento.isoformat(),
        "codSexo": sexo,
        "codPaisResidencia": "170",
        "codMunicipioResidencia": rng.choice(MUNICIPIOS),
        "codZonaTerritorialResidencia": rng.choice(["01", "02"]),
        "incapacidad": "NO",
        "consecutivo": consecutivo,
        "codPaisOrigen": "170",
        "servicios": {"consultas": consultas, "procedimientos": procedimientos},
    }


# --- Errores inyectados ----------------------------------------------------------------
# Cada función daña una factura válida de una forma que el validador debe detectar.

def error_embarazo_en_hombre(rng, rips, hoy):
    usuario = rng.choice(rips["usuarios"])
    usuario["codSexo"] = "H"
    usuario["servicios"]["consultas"][0]["codDiagnosticoPrincipal"] = "O800"


def error_moderador_mayor(rng, rips, hoy):
    consulta = rng.choice(rips["usuarios"])["servicios"]["consultas"][0]
    consulta["conceptoRecaudo"] = "02"
    consulta["valorPagoModerador"] = consulta["vrServicio"] + 10_000


def error_cups_incompleto(rng, rips, hoy):
    consulta = rng.choice(rips["usuarios"])["servicios"]["consultas"][0]
    consulta["codConsulta"] = consulta["codConsulta"][:4]


def error_atencion_futura(rng, rips, hoy):
    consulta = rng.choice(rips["usuarios"])["servicios"]["consultas"][0]
    consulta["fechaInicioAtencion"] = (hoy + timedelta(days=30)).strftime("%Y-%m-%d 08:00")


def error_usuario_duplicado(rng, rips, hoy):
    copia = copy.deepcopy(rips["usuarios"][0])
    copia["consecutivo"] = len(rips["usuarios"]) + 1
    rips["usuarios"].append(copia)


def error_municipio_vacio(rng, rips, hoy):
    rng.choice(rips["usuarios"])["codMunicipioResidencia"] = None


def error_cedula_en_menor(rng, rips, hoy):
    usuario = rng.choice(rips["usuarios"])
    usuario["tipoDocumentoIdentificacion"] = "CC"
    usuario["fechaNacimiento"] = (hoy - timedelta(days=10 * 365)).isoformat()
    for servicio in usuario["servicios"]["consultas"] + usuario["servicios"]["procedimientos"]:
        servicio["fechaInicioAtencion"] = fecha_atencion(rng, hoy - timedelta(days=60), hoy)


ERRORES = [
    error_embarazo_en_hombre,
    error_moderador_mayor,
    error_cups_incompleto,
    error_atencion_futura,
    error_usuario_duplicado,
    error_municipio_vacio,
    error_cedula_en_menor,
]


def generar_factura(rng: random.Random, numero: int, tasa_errores: float, hoy: date) -> tuple[dict, list[str]]:
    rips = {
        "numDocumentoIdObligado": "900123456",
        "numFactura": f"FEV{numero:05d}",
        "tipoNota": None,
        "numNota": None,
        "usuarios": [generar_usuario(rng, i + 1, hoy) for i in range(rng.randint(1, 5))],
    }
    inyectados = []
    if rng.random() < tasa_errores:
        for dano in rng.sample(ERRORES, k=rng.randint(1, 3)):
            dano(rng, rips, hoy)
            inyectados.append(dano.__name__.removeprefix("error_"))
    return rips, inyectados


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--facturas", type=int, default=20, help="cantidad de archivos a generar")
    parser.add_argument("--tasa-errores", type=float, default=0.3, help="fracción de facturas con errores (0 a 1)")
    parser.add_argument("--salida", type=Path, default=Path("datos"), help="carpeta de destino")
    parser.add_argument("--semilla", type=int, default=None, help="semilla para obtener siempre los mismos datos")
    args = parser.parse_args()

    rng = random.Random(args.semilla)
    hoy = date.today()
    args.salida.mkdir(parents=True, exist_ok=True)

    con_errores = 0
    for numero in range(1, args.facturas + 1):
        rips, inyectados = generar_factura(rng, numero, args.tasa_errores, hoy)
        nombre = args.salida / f"{rips['numFactura']}.json"
        nombre.write_text(json.dumps(rips, ensure_ascii=False, indent=2), encoding="utf-8")
        if inyectados:
            con_errores += 1
            print(f"{nombre.name}: {', '.join(inyectados)}")

    print(f"\n{args.facturas} facturas en {args.salida}/ ({con_errores} con errores inyectados)")


if __name__ == "__main__":
    main()
