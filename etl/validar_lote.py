"""Envía una carpeta de RIPS a la API y consolida los resultados para analizarlos.

Genera en la carpeta de reporte:
    facturas.csv   una fila por factura (estado, totales)
    hallazgos.csv  una fila por hallazgo, lista para Power BI
    resumen.xlsx   las dos tablas anteriores más los hallazgos agrupados por regla

Uso:
    python validar_lote.py datos --api http://localhost:8080 --reporte reporte
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

import pandas as pd


def validar(api: str, archivo: Path) -> dict:
    peticion = urllib.request.Request(
        f"{api}/api/v1/validaciones",
        data=archivo.read_bytes(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(peticion, timeout=30) as respuesta:
        return json.load(respuesta)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("carpeta", type=Path, help="carpeta con los archivos .json")
    parser.add_argument("--api", default="http://localhost:8080", help="URL base de la API")
    parser.add_argument("--reporte", type=Path, default=Path("reporte"), help="carpeta de salida")
    args = parser.parse_args()

    archivos = sorted(args.carpeta.glob("*.json"))
    if not archivos:
        sys.exit(f"No hay archivos .json en {args.carpeta}")

    facturas, hallazgos, fallidos = [], [], []
    for archivo in archivos:
        try:
            resultado = validar(args.api.rstrip("/"), archivo)
        except urllib.error.HTTPError as e:
            fallidos.append((archivo.name, f"HTTP {e.code}"))
            continue
        except urllib.error.URLError as e:
            sys.exit(f"No se pudo conectar con la API en {args.api}: {e.reason}")

        detalle = resultado.pop("hallazgos")
        facturas.append({"archivo": archivo.name, **resultado})
        hallazgos.extend({"archivo": archivo.name, "idValidacion": resultado["id"], "numFactura": resultado["numFactura"], **h}
                         for h in detalle)

    df_facturas = pd.DataFrame(facturas)
    df_hallazgos = pd.DataFrame(hallazgos, columns=["archivo", "idValidacion", "numFactura", "codigo", "severidad", "ruta", "mensaje", "valor"])
    # Tipo de servicio y campo salen de la ruta: usuarios[0].servicios.consultas[1].codConsulta
    df_hallazgos["tipoServicio"] = df_hallazgos["ruta"].str.extract(r"servicios\.(\w+)\[")[0].fillna("usuario")
    df_hallazgos["campo"] = df_hallazgos["ruta"].str.split(".").str[-1].str.replace(r"\[\d+\]$", "", regex=True)

    por_regla = (df_hallazgos.groupby(["codigo", "severidad"])
                 .agg(hallazgos=("ruta", "size"), facturas=("numFactura", "nunique"))
                 .reset_index()
                 .sort_values("hallazgos", ascending=False))

    args.reporte.mkdir(parents=True, exist_ok=True)
    df_facturas.to_csv(args.reporte / "facturas.csv", index=False, encoding="utf-8-sig")
    df_hallazgos.to_csv(args.reporte / "hallazgos.csv", index=False, encoding="utf-8-sig")
    with pd.ExcelWriter(args.reporte / "resumen.xlsx") as excel:
        df_facturas.to_excel(excel, sheet_name="facturas", index=False)
        df_hallazgos.to_excel(excel, sheet_name="hallazgos", index=False)
        por_regla.to_excel(excel, sheet_name="por_regla", index=False)

    print(df_facturas["estado"].value_counts().to_string(), end="\n\n")
    print(por_regla.to_string(index=False))
    for nombre, motivo in fallidos:
        print(f"No validado {nombre}: {motivo}")
    print(f"\nReporte en {args.reporte}/")


if __name__ == "__main__":
    main()
