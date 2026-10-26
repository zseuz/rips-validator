"""Descarga las tablas de referencia de SISPRO que usa el validador y las deja en CSV.

Fuente: la consulta pública de tablas de referencia del Ministerio de Salud
(https://web.sispro.gov.co/WebPublico/Consultas/ConsultarReferenciaBasica.aspx). La página es
ASP.NET: cada página de resultados se pide reenviando el estado (__VIEWSTATE) de la anterior.

Escribe en la carpeta de salida un CSV por tabla (codigo, nombre, habilitado y las columnas
extra que interesan) y metadatos.json con la fecha de descarga. La API empaqueta esa carpeta.

Uso:
    python descargar_tablas_sispro.py --salida ../catalogos
    python descargar_tablas_sispro.py --salida ../catalogos --tablas CIE10 Sexo
"""

from __future__ import annotations

import argparse
import csv
import http.cookiejar
import json
import sys
import time
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path

URL = "https://web.sispro.gov.co/WebPublico/Consultas/ConsultarDetalleReferenciaBasica.aspx"
PREFIJO = "ctl00$cntContenido$"
FILAS_POR_PAGINA = "2000"

# Tabla de SISPRO → columnas extra que se conservan (el resto de Extra_* no se usa).
TABLAS: dict[str, list[str]] = {
    "CIE10": ["AplicaASexo", "EdadMinima", "EdadMaxima"],
    "CUPSRips": ["UsoCodigoCUP"],
    "TipoIdPISIS": [],
    "Sexo": [],
    "Pais": [],
    "Municipio": [],
    "ZonaVersion2": [],
    "RIPSTipoUsuarioVersion2": [],
    "TipoNota": [],
    "conceptoRecaudo": [],
    "ModalidadAtencion": [],
    "GrupoServicios": [],
    "RIPSFinalidadConsultaVersion2": [],
    "RIPSCausaExternaVersion2": [],
    "RIPSTipoDiagnosticoPrincipalVersion2": [],
    "ViaIngresoUsuario": [],
}


class PaginaSispro(HTMLParser):
    """Extrae los campos ocultos del formulario y la grilla de resultados."""

    def __init__(self, tabla: str) -> None:
        super().__init__()
        self.tabla = tabla
        self.ocultos: dict[str, str] = {}
        self.encabezados: list[str] = []
        self.filas: list[list[str]] = []
        self.hay_siguiente = False
        self._celda: list[str] | None = None
        self._fila: list[str] | None = None
        self._en_th = False

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if tag == "input" and a.get("type") == "hidden" and a.get("name"):
            self.ocultos[a["name"]] = a.get("value", "")
        elif tag == "a" and "imgNext" in (a.get("href") or ""):
            self.hay_siguiente = True
        elif tag == "tr":
            self._fila = []
        elif tag in ("td", "th"):
            self._celda = []
            self._en_th = tag == "th"

    def handle_data(self, data):
        if self._celda is not None:
            self._celda.append(data)

    def handle_endtag(self, tag):
        if tag in ("td", "th") and self._celda is not None:
            texto = " ".join("".join(self._celda).split())
            if self._en_th:
                self.encabezados.append(texto)
            elif self._fila is not None:
                self._fila.append(texto)
            self._celda = None
        elif tag == "tr" and self._fila:
            if self._fila[0] == self.tabla:
                self.filas.append(self._fila)
            self._fila = None


class Cliente:
    def __init__(self) -> None:
        self.abridor = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.abridor.addheaders = [("User-Agent", "rips-validator/0.1 (+https://github.com/zseuz/rips-validator)")]

    def pedir(self, url: str, datos: dict[str, str] | None = None) -> str:
        cuerpo = urllib.parse.urlencode(datos).encode() if datos is not None else None
        with self.abridor.open(url, data=cuerpo, timeout=120) as respuesta:
            return respuesta.read().decode("utf-8", errors="replace")


def leer(tabla: str, html: str) -> PaginaSispro:
    pagina = PaginaSispro(tabla)
    pagina.feed(html)
    return pagina


def postback(pagina: PaginaSispro, evento: str) -> dict[str, str]:
    datos = dict(pagina.ocultos)
    datos.update({
        "__EVENTTARGET": PREFIJO + evento,
        "__EVENTARGUMENT": "",
        PREFIJO + "dpggrvTablaReferencia$ddlPageSize": FILAS_POR_PAGINA,
        PREFIJO + "txtBuscarCodigo": "",
        PREFIJO + "txtBuscarNombre": "",
        PREFIJO + "TxtEmail": "",
    })
    return datos


def descargar(cliente: Cliente, tabla: str) -> tuple[list[str], list[list[str]]]:
    url = f"{URL}?Code={urllib.parse.quote(tabla)}"
    pagina = leer(tabla, cliente.pedir(url))
    pagina = leer(tabla, cliente.pedir(url, postback(pagina, "dpggrvTablaReferencia$ddlPageSize")))
    encabezados, filas = pagina.encabezados, list(pagina.filas)
    while pagina.hay_siguiente and pagina.filas:
        time.sleep(0.5)  # la consulta es un servicio público: sin ráfagas
        pagina = leer(tabla, cliente.pedir(url, postback(pagina, "dpggrvTablaReferencia$imgNext")))
        filas.extend(pagina.filas)
        print(f"  {tabla}: {len(filas)} filas…", flush=True)
    return encabezados, filas


def normalizar(tabla: str, encabezados: list[str], filas: list[list[str]]) -> list[dict[str, str]]:
    # "Extra_I:AplicaASexo" → "AplicaASexo"; la grilla tiene columnas de selección antes de los datos.
    nombres = [e.split(":")[-1] for e in encabezados]
    desfase = len(filas[0]) - len(nombres) if filas else 0
    columnas = ["Codigo", "Nombre", "Habilitado", *TABLAS[tabla]]
    faltantes = [c for c in columnas if c not in nombres]
    if faltantes:
        raise SystemExit(f"{tabla}: SISPRO cambió el formato, faltan las columnas {faltantes}")
    indices = {c: nombres.index(c) + desfase for c in columnas}

    vistos: dict[str, dict[str, str]] = {}
    for fila in filas:
        registro = {c.lower() if c in ("Codigo", "Nombre", "Habilitado") else c: fila[i] for c, i in indices.items()}
        vistos[registro["codigo"]] = registro  # algunas tablas repiten código; gana el último
    return sorted(vistos.values(), key=lambda r: r["codigo"])


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--salida", type=Path, default=Path("catalogos"), help="carpeta de destino")
    parser.add_argument("--tablas", nargs="*", default=list(TABLAS), choices=list(TABLAS), help="tablas a descargar")
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8")  # la consola de Windows no es UTF-8 por defecto

    args.salida.mkdir(parents=True, exist_ok=True)
    archivo_meta = args.salida / "metadatos.json"
    metadatos = json.loads(archivo_meta.read_text(encoding="utf-8")) if archivo_meta.exists() else {"tablas": {}}
    metadatos["fuente"] = URL.replace("ConsultarDetalleReferenciaBasica", "ConsultarReferenciaBasica")

    cliente = Cliente()
    for tabla in args.tablas:
        print(f"Descargando {tabla}…", flush=True)
        encabezados, filas = descargar(cliente, tabla)
        registros = normalizar(tabla, encabezados, filas)
        with open(args.salida / f"{tabla}.csv", "w", newline="", encoding="utf-8") as f:
            escritor = csv.DictWriter(f, fieldnames=list(registros[0]), lineterminator="\n")
            escritor.writeheader()
            escritor.writerows(registros)
        metadatos["tablas"][tabla] = {
            "registros": len(registros),
            "descargada": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        }
        print(f"  {tabla}: {len(registros)} códigos")
        time.sleep(0.5)

    archivo_meta.write_text(json.dumps(metadatos, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"\nListo: {len(args.tablas)} tablas en {args.salida}/")


if __name__ == "__main__":
    main()
