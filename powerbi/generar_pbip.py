"""Genera el proyecto de Power BI del validador (RipsValidator.pbip).

Crea un proyecto PBIP (formato de texto de Power BI Desktop) con:
  * Modelo semántico (model.bim): Validaciones, Hallazgos y Reglas, leídas en modo importación de
    los CSV de la API (/api/v1/exportaciones/*.csv), relaciones en estrella y medidas DAX.
  * Reporte (report.json): 5 páginas con tarjetas, gráficos, tablas y segmentadores.

El origen es el parámetro UrlApi (por defecto http://localhost:8080). La API entrega este mismo
proyecto en /api/v1/exportaciones/power-bi.zip con UrlApi apuntando a ella.

Uso:
    python powerbi/generar_pbip.py
    -> abrir powerbi/RipsValidator.pbip en Power BI Desktop con la API encendida y pulsar Actualizar.
"""

import json
import uuid
from pathlib import Path

NOMBRE = "RipsValidator"
SALIDA = Path(__file__).resolve().parent
URL_API = "http://localhost:8080"
MEDIDAS = "_Medidas"
V, H, R = "Validaciones", "Hallazgos", "Reglas"

# ============================================================================ tablas
# (columna, tipo del modelo, tipo de Power Query, formato, oculta)
COLUMNAS = {
    V: [("IdValidacion", "int64", "Int64.Type", None, True),
        ("Factura", "string", "type text", None, False),
        ("Obligado", "string", "type text", None, False),
        ("Estado", "string", "type text", None, False),
        ("FechaValidacion", "dateTime", "type datetime", "dd/mm/yyyy hh:nn", False),
        ("Usuarios", "int64", "Int64.Type", "#,0", False),
        ("Servicios", "int64", "Int64.Type", "#,0", False),
        ("Errores", "int64", "Int64.Type", "#,0", False),
        ("Advertencias", "int64", "Int64.Type", "#,0", False)],
    H: [("IdValidacion", "int64", "Int64.Type", None, True),
        ("Orden", "int64", "Int64.Type", None, True),
        ("Regla", "string", "type text", None, True),
        ("Severidad", "string", "type text", None, True),
        ("Usuario", "int64", "Int64.Type", None, False),
        ("TipoServicio", "string", "type text", None, False),
        ("Campo", "string", "type text", None, False),
        ("Ruta", "string", "type text", None, False),
        ("Mensaje", "string", "type text", None, False),
        ("Valor", "string", "type text", None, False),
        ("TablaReferencia", "string", "type text", None, False),
        ("NombreReferencia", "string", "type text", None, False)],
    R: [("Regla", "string", "type text", None, False),
        ("Severidad", "string", "type text", None, False),
        ("Titulo", "string", "type text", None, False),
        ("Grupo", "string", "type text", None, False)],
}
ARCHIVO = {V: "validaciones.csv", H: "hallazgos.csv", R: "reglas.csv"}

# ============================================================================ medidas DAX
# (nombre, expresión, formato, carpeta, descripción)
ESTADO = f"{V}[Estado]"
DAX = [
    ("Validaciones", f"COUNTROWS({V})", "#,0", "1. Validaciones", "Archivos RIPS validados."),
    ("Válidas", f'CALCULATE([Validaciones], {ESTADO} = "Válido")', "#,0", "1. Validaciones",
     "Validaciones sin errores ni advertencias."),
    ("Con advertencias", f'CALCULATE([Validaciones], {ESTADO} = "Con advertencias")', "#,0", "1. Validaciones",
     "Se pueden enviar, pero tienen advertencias que suelen terminar en glosas."),
    ("Rechazadas", f'CALCULATE([Validaciones], {ESTADO} = "Rechazado")', "#,0", "1. Validaciones",
     "Validaciones con al menos un error: el Ministerio las rechazaría."),
    ("% rechazo", "DIVIDE([Rechazadas], [Validaciones])", "0.0%", "1. Validaciones",
     "Rechazadas / validaciones."),
    ("% listas para enviar", "DIVIDE([Válidas] + [Con advertencias], [Validaciones])", "0.0%", "1. Validaciones",
     "Validaciones sin errores / validaciones."),
    ("Usuarios reportados", f"SUM({V}[Usuarios])", "#,0", "1. Validaciones", "Usuarios en los archivos validados."),
    ("Servicios reportados", f"SUM({V}[Servicios])", "#,0", "1. Validaciones",
     "Consultas y procedimientos en los archivos validados."),
    ("Última validación", f'IF(ISEMPTY({V}), "Sin datos", FORMAT(MAX({V}[FechaValidacion]), "dd/mm/yyyy hh:nn"))', "",
     "1. Validaciones", "Fecha y hora de la validación más reciente."),
    ("Hallazgos", f"COUNTROWS({H})", "#,0", "2. Hallazgos", "Errores y advertencias encontrados."),
    ("Errores", f'CALCULATE([Hallazgos], {R}[Severidad] = "ERROR")', "#,0", "2. Hallazgos",
     "Hallazgos que impiden el envío."),
    ("Advertencias", f'CALCULATE([Hallazgos], {R}[Severidad] = "ADVERTENCIA")', "#,0", "2. Hallazgos",
     "Hallazgos que no impiden el envío."),
    ("Errores por factura", "DIVIDE([Errores], [Validaciones])", "#,0.0", "2. Hallazgos",
     "Errores promedio por archivo validado."),
    ("Facturas con hallazgos", f"DISTINCTCOUNT({H}[IdValidacion])", "#,0", "2. Hallazgos",
     "Validaciones con al menos un hallazgo del filtro actual (regla, severidad, campo…)."),
    ("% facturas con hallazgos", "DIVIDE([Facturas con hallazgos], [Validaciones])", "0.0%", "2. Hallazgos",
     "Qué parte de las facturas tiene el hallazgo seleccionado."),
    ("Hallazgos en códigos", f"CALCULATE([Hallazgos], NOT ISBLANK({H}[TablaReferencia]))", "#,0", "3. Códigos",
     "Hallazgos sobre diagnósticos (CIE-10), CUPS, municipios o países."),
    ("Códigos distintos", f"CALCULATE(DISTINCTCOUNT({H}[Valor]), NOT ISBLANK({H}[TablaReferencia]))", "#,0",
     "3. Códigos", "Códigos diferentes con al menos un hallazgo."),
    # Solo cuenta hallazgos sobre códigos: en una tabla, las filas que no son códigos quedan en blanco y se ocultan.
    ("Facturas con códigos", f"CALCULATE([Facturas con hallazgos], NOT ISBLANK({H}[TablaReferencia]))", "#,0",
     "3. Códigos", "Facturas con al menos un hallazgo sobre un código de SISPRO."),
]
FORMATO_MEDIDA = {nombre: formato for nombre, _, formato, _, _ in DAX}


# ============================================================================ modelo
def consulta_m(tabla):
    columnas = COLUMNAS[tabla]
    tipos = ", ".join(f'{{"{c}", {tipo_m}}}' for c, _, tipo_m, _, _ in columnas)
    pasos = [
        "let",
        f'    Origen = Csv.Document(Web.Contents(UrlApi, [RelativePath = "api/v1/exportaciones/{ARCHIVO[tabla]}"]), '
        '[Delimiter = ",", Encoding = 65001, QuoteStyle = QuoteStyle.Csv]),',
        "    Encabezados = Table.PromoteHeaders(Origen, [PromoteAllScalars = true]),",
        "    // Los campos vacíos del CSV llegan como texto vacío: se vuelven nulos antes de tipar.",
        '    Nulos = Table.TransformColumns(Encabezados, {}, each if _ = "" then null else _),',
        f"    Tipos = Table.TransformColumnTypes(Nulos, {{{tipos}}}, \"en-US\")",
    ]
    final = "Tipos"
    if tabla == V:
        pasos[-1] += ","
        pasos.append('    ConFecha = Table.AddColumn(Tipos, "Fecha", each DateTime.Date([FechaValidacion]), type date)')
        final = "ConFecha"
    return pasos + ["in", f"    {final}"]


def tabla_modelo(nombre):
    cols = []
    for c, tipo, _, formato, oculta in COLUMNAS[nombre]:
        col = {"name": c, "dataType": tipo, "sourceColumn": c, "summarizeBy": "none"}
        if formato:
            col["formatString"] = formato
        if oculta:
            col["isHidden"] = True
        cols.append(col)
    if nombre == V:
        cols.append({"name": "Fecha", "dataType": "dateTime", "sourceColumn": "Fecha", "summarizeBy": "none",
                     "formatString": "dd/mm/yyyy"})
    if nombre == R:
        next(c for c in cols if c["name"] == "Titulo")["sortByColumn"] = "Regla"
    return {"name": nombre, "columns": cols, "partitions": [{
        "name": nombre, "mode": "import", "source": {"type": "m", "expression": consulta_m(nombre)}}]}


def modelo():
    tablas = [tabla_modelo(t) for t in (V, H, R)]
    tablas.append({
        "name": MEDIDAS,
        "columns": [{"name": "Medida", "dataType": "string", "isHidden": True, "type": "calculatedTableColumn",
                     "sourceColumn": "[Medida]"}],
        "partitions": [{"name": MEDIDAS, "mode": "import",
                        "source": {"type": "calculated", "expression": "ROW(\"Medida\", BLANK())"}}],
        "measures": [{"name": n, "expression": e, **({"formatString": f} if f else {}), "displayFolder": carpeta,
                      "description": d} for n, e, f, carpeta, d in DAX],
    })
    rel = lambda desde, col, hacia, col2: {"name": str(uuid.uuid5(uuid.NAMESPACE_DNS, f"{desde}.{col}-{hacia}")),
                                           "fromTable": desde, "fromColumn": col, "toTable": hacia, "toColumn": col2}
    return {
        "compatibilityLevel": 1567,
        "model": {
            "culture": "es-ES",
            "dataAccessOptions": {"legacyRedirects": True, "returnErrorValuesAsNull": True},
            "defaultPowerBIDataSourceVersion": "powerBI_V3",
            "sourceQueryCulture": "es-CO",
            "tables": tablas,
            "relationships": [rel(H, "IdValidacion", V, "IdValidacion"), rel(H, "Regla", R, "Regla")],
            "expressions": [{
                "name": "UrlApi", "kind": "m", "description": "Dirección de la API del validador (sin / al final).",
                # ReportePowerBi.java reemplaza esta URL al armar el .zip de descarga: no cambiar el formato.
                "expression": f"\"{URL_API}\" meta [IsParameterQuery=true, Type=\"Text\", IsParameterQueryRequired=true]"}],
            "annotations": [{"name": "PBI_QueryOrder", "value": json.dumps(["UrlApi", V, H, R])},
                            {"name": "__PBI_TimeIntelligenceEnabled", "value": "0"}],
        },
    }


# ============================================================================ reporte
def _lit(v):
    return {"expr": {"Literal": {"Value": v}}}


class Pagina:
    def __init__(self, nombre, titulo):
        self.nombre, self.titulo, self.visuales, self.z = nombre, titulo, [], 0

    def _agregar(self, x, y, w, h, single):
        self.z += 1
        cfg = {"name": uuid.uuid5(uuid.NAMESPACE_DNS, f"{self.nombre}-{self.z}").hex[:20],
               "layouts": [{"id": 0, "position": {"x": x, "y": y, "z": self.z, "width": w, "height": h}}],
               "singleVisual": single}
        self.visuales.append({"x": x, "y": y, "z": self.z, "width": w, "height": h,
                              "config": json.dumps(cfg, ensure_ascii=False), "filters": "[]"})

    def texto(self, x, y, w, h, parrafos, fondo=None, borde=None):
        """parrafos: lista de (texto, tamaño_pt, negrita) o (texto, tamaño_pt, negrita, color_hex)."""
        def estilo(p):
            _, s, b, color = (*p, None)[:4]
            return {"fontSize": f"{s}pt", **({"fontWeight": "bold"} if b else {}), **({"color": color} if color else {})}
        single = {"visualType": "textbox", "drillFilterOtherVisuals": True, "objects": {
            "general": [{"properties": {"paragraphs": [
                {"textRuns": [{"value": p[0], "textStyle": estilo(p)}]} for p in parrafos]}}]}}
        color = lambda c: {"solid": {"color": _lit(f"'{c}'")}}
        vc = {}
        if fondo:
            vc["background"] = [{"properties": {"show": _lit("true"), "color": color(fondo), "transparency": _lit("0D")}}]
        if borde:
            vc["border"] = [{"properties": {"show": _lit("true"), "color": color(borde), "radius": _lit("8D")}}]
        if vc:
            single["vcObjects"] = vc
        self._agregar(x, y, w, h, single)

    def visual(self, tipo, x, y, w, h, roles, titulo=None, objetos=None):
        """roles: {"Category": [campo], "Y": [campo], ...}; campo = ("m", medida) o ("c", tabla, columna)."""
        alias, desde, select, proy = {}, [], [], {}
        for rol, campos in roles.items():
            proy[rol] = []
            for campo in campos:
                entidad = MEDIDAS if campo[0] == "m" else campo[1]
                prop = campo[1] if campo[0] == "m" else campo[2]
                if entidad not in alias:
                    alias[entidad] = f"t{len(alias)}"
                    desde.append({"Name": alias[entidad], "Entity": entidad, "Type": 0})
                ref = f"{entidad}.{prop}"
                clave = "Measure" if campo[0] == "m" else "Column"
                if not any(s["Name"] == ref for s in select):
                    select.append({clave: {"Expression": {"SourceRef": {"Source": alias[entidad]}}, "Property": prop},
                                   "Name": ref, "NativeReferenceName": prop})
                proy[rol].append({"queryRef": ref, **({"active": True} if rol == "Category" else {})})
        single = {"visualType": tipo, "projections": proy,
                  "prototypeQuery": {"Version": 2, "From": desde, "Select": select},
                  "drillFilterOtherVisuals": True}
        if objetos:
            single["objects"] = objetos
        if titulo:
            single["vcObjects"] = {"title": [{"properties": {"show": _lit("true"), "text": _lit(f"'{titulo}'")}}]}
        self._agregar(x, y, w, h, single)

    def tarjeta(self, x, y, w, h, medida, titulo):
        self.visual("card", x, y, w, h, {"Values": [("m", medida)]}, titulo,
                    {"labels": [{"properties": {"fontSize": _lit("20D")}}],
                     "categoryLabels": [{"properties": {"show": _lit("false")}}]})

    def segmentador(self, x, y, w, h, tabla, columna, titulo, modo="Dropdown"):
        objetos = {"data": [{"properties": {"mode": _lit(f"'{modo}'")}}],
                   "header": [{"properties": {"show": _lit("false")}}]}
        self.visual("slicer", x, y, w, h, {"Values": [("c", tabla, columna)]}, titulo, objetos)

    def seccion(self, orden):
        return {"config": "{}", "displayName": self.titulo, "displayOption": 1, "filters": "[]",
                "height": 720.0, "width": 1280.0, "name": self.nombre, "ordinal": orden,
                "visualContainers": self.visuales}


AZUL, VERDE = "#0D4F4A", "#0D7A72"


def encabezado(p, titulo, nota):
    p.texto(10, 0, 1260, 62, [(titulo, 16, True, AZUL), (nota, 9, False)])


def segmentadores(p, campos):
    """Fila de segmentadores bajo el encabezado; el primero es siempre el rango de fechas."""
    p.segmentador(10, 62, 300, 63, V, "Fecha", "Fecha de validación", modo="Between")
    ancho = (1260 - 310) // len(campos)
    for i, (tabla, columna, titulo) in enumerate(campos):
        p.segmentador(320 + i * ancho, 62, ancho - 10, 63, tabla, columna, titulo)


def tarjetas(p, medidas, y=130, alto=95):
    ancho = (1260 - 10 * (len(medidas) - 1)) // len(medidas)
    for i, (medida, titulo) in enumerate(medidas):
        p.tarjeta(10 + i * (ancho + 10), y, ancho, alto, medida, titulo)


def reporte():
    paginas = []

    # ---- 1. Resumen -------------------------------------------------------------------------
    p = Pagina("ReportSection01", "Resumen")
    encabezado(p, "Validación de RIPS – Resumen",
               "Archivos RIPS (Resolución 2275 de 2023) validados contra las reglas y las tablas de referencia de "
               "SISPRO. Datos de la API del validador: pulse Actualizar para traer las validaciones nuevas.")
    segmentadores(p, [(V, "Estado", "Estado"), (V, "Obligado", "Obligado (NIT)"), (V, "Factura", "Factura")])
    tarjetas(p, [("Validaciones", "Validaciones"), ("% rechazo", "% rechazo"),
                 ("% listas para enviar", "% listas para enviar"), ("Errores", "Errores"),
                 ("Advertencias", "Advertencias"), ("Errores por factura", "Errores por factura"),
                 ("Servicios reportados", "Servicios reportados")])
    p.visual("donutChart", 10, 235, 400, 230, {"Category": [("c", V, "Estado")], "Y": [("m", "Validaciones")]},
             "Validaciones por estado")
    p.visual("clusteredBarChart", 420, 235, 430, 230, {"Category": [("c", R, "Titulo")], "Y": [("m", "Hallazgos")]},
             "Hallazgos por regla")
    p.visual("clusteredBarChart", 860, 235, 410, 230, {"Category": [("c", H, "Campo")], "Y": [("m", "Errores")]},
             "Campos con más errores")
    p.visual("lineChart", 10, 475, 630, 235,
             {"Category": [("c", V, "Fecha")], "Y": [("m", "Validaciones"), ("m", "Rechazadas")]},
             "Validaciones y rechazos por día")
    p.visual("clusteredColumnChart", 650, 475, 620, 235,
             {"Category": [("c", H, "TipoServicio")], "Y": [("m", "Errores"), ("m", "Advertencias")]},
             "Hallazgos por tipo de registro")
    paginas.append(p)

    # ---- 2. Reglas --------------------------------------------------------------------------
    p = Pagina("ReportSection02", "Reglas")
    encabezado(p, "Hallazgos por regla",
               "Qué reglas fallan más y en cuántas facturas. Los segmentadores de regla, severidad y tipo de "
               "registro filtran los hallazgos; los de fecha y estado, las validaciones.")
    segmentadores(p, [(R, "Grupo", "Grupo de reglas"), (R, "Severidad", "Severidad"),
                      (H, "TipoServicio", "Tipo de registro")])
    tarjetas(p, [("Hallazgos", "Hallazgos"), ("Facturas con hallazgos", "Facturas con hallazgos"),
                 ("% facturas con hallazgos", "% facturas con hallazgos"), ("Códigos distintos", "Códigos distintos")])
    p.visual("tableEx", 10, 235, 760, 475,
             {"Values": [("c", R, "Regla"), ("c", R, "Titulo"), ("c", R, "Severidad"), ("c", R, "Grupo"),
                         ("m", "Hallazgos"), ("m", "Facturas con hallazgos")]},
             "Detalle por regla")
    p.visual("clusteredBarChart", 780, 235, 490, 230, {"Category": [("c", R, "Grupo")], "Y": [("m", "Hallazgos")]},
             "Hallazgos por grupo de reglas")
    p.visual("donutChart", 780, 475, 490, 235, {"Category": [("c", R, "Severidad")], "Y": [("m", "Hallazgos")]},
             "Errores frente a advertencias")
    paginas.append(p)

    # ---- 3. Códigos -------------------------------------------------------------------------
    p = Pagina("ReportSection03", "Códigos")
    encabezado(p, "Diagnósticos, CUPS y municipios con más hallazgos",
               "Con el nombre oficial de la tabla de referencia de SISPRO. '(no existe en SISPRO)' marca códigos "
               "que no están en la tabla: suelen ser errores de digitación.")
    segmentadores(p, [(H, "TablaReferencia", "Tabla de SISPRO"), (R, "Titulo", "Regla")])
    p.visual("clusteredBarChart", 10, 130, 630, 580,
             {"Category": [("c", H, "NombreReferencia")], "Y": [("m", "Hallazgos en códigos")]},
             "Códigos con más hallazgos")
    p.visual("tableEx", 650, 130, 620, 580,
             {"Values": [("c", H, "TablaReferencia"), ("c", H, "Valor"), ("c", H, "NombreReferencia"),
                         ("m", "Hallazgos en códigos"), ("m", "Facturas con códigos")]},
             "Detalle por código")
    paginas.append(p)

    # ---- 4. Detalle -------------------------------------------------------------------------
    p = Pagina("ReportSection04", "Detalle")
    encabezado(p, "Detalle de hallazgos",
               "Un renglón por hallazgo, con la factura, el usuario, el campo y el valor recibido. Filtre por factura "
               "para preparar la corrección de un archivo.")
    segmentadores(p, [(V, "Factura", "Factura"), (R, "Severidad", "Severidad"), (R, "Regla", "Regla"),
                      (H, "TipoServicio", "Tipo de registro")])
    p.visual("tableEx", 10, 130, 1260, 580,
             {"Values": [("c", V, "Factura"), ("c", V, "FechaValidacion"), ("c", R, "Regla"), ("c", R, "Severidad"),
                         ("c", H, "Usuario"), ("c", H, "TipoServicio"), ("c", H, "Campo"), ("c", H, "Valor"),
                         ("c", H, "Mensaje")]},
             "Hallazgos")
    paginas.append(p)

    # ---- 5. Cómo usar -------------------------------------------------------------------------
    p = Pagina("ReportSection05", "Cómo usar")
    encabezado(p, "Cómo usar este reporte", "Origen de los datos, actualización y definición de los indicadores.")
    ancho, alto = 413, 150
    for i, (nombre, formula, texto, es_nota) in enumerate(GUIA):
        x, y = 10 + (i % 3) * (ancho + 10), 70 + (i // 3) * (alto + 8)
        acento = "#8A6D00" if es_nota else AZUL
        parrafos = [(nombre, 12, True, acento)]
        if formula:
            parrafos.append((formula, 9, True, "#595959"))
        parrafos.append((texto, 9, False, "#262626"))
        p.texto(x, y, ancho, alto, parrafos, fondo="#FFF8E1" if es_nota else "#EEF7F6",
                borde="#E6C35C" if es_nota else "#9CCFC9")
    p.tarjeta(10, 640, 400, 70, "Última validación", "Última validación cargada")
    paginas.append(p)

    return {"config": json.dumps({"version": "5.43", "themeCollection": {}, "activeSectionIndex": 0,
                                  "linguisticSchemaSyncVersion": 0}),
            "layoutOptimization": 0,
            "sections": [pg.seccion(i) for i, pg in enumerate(paginas)]}


GUIA = [
    ("Origen de los datos", "Parámetro UrlApi",
     "Las tablas se leen de la API del validador (/api/v1/exportaciones/*.csv). Para cambiar la dirección: "
     "Transformar datos > Editar parámetros > UrlApi.", False),
    ("Actualizar", "Inicio > Actualizar",
     "Trae las validaciones nuevas. La API debe estar encendida. Si Power BI pide el tipo de acceso a la "
     "dirección web, elija Anónimo.", False),
    ("% rechazo", "Rechazadas / validaciones",
     "Facturas con al menos un error que el Ministerio rechazaría. Es el indicador principal de calidad del RIPS "
     "antes del envío.", False),
    ("% listas para enviar", "(Válidas + con advertencias) / validaciones",
     "Facturas sin errores. Las advertencias no bloquean el envío, pero suelen terminar en glosas.", False),
    ("Errores por factura", "Errores / validaciones",
     "Esfuerzo de corrección promedio. Sirve para comparar prestadores o meses.", False),
    ("Facturas con hallazgos", "Validaciones distintas con el hallazgo filtrado",
     "Con una regla seleccionada, dice cuántas facturas afecta: ayuda a priorizar qué capacitar o qué "
     "corregir en el sistema de origen.", False),
    ("Tablas de SISPRO", "CIE-10, CUPS, municipios, países y 12 más",
     "Los códigos se validan contra las tablas de referencia del Ministerio que trae la API; la página "
     "Códigos muestra su nombre oficial.", False),
    ("Datos personales", "",
     "La API no guarda el RIPS, solo el resumen y los hallazgos: este reporte no tiene nombres ni documentos "
     "de pacientes.", True),
    ("Proyecto de portafolio", "",
     "No reemplaza la validación oficial del Ministerio (MUV). Código en github.com/zseuz/rips-validator.", True),
]


# ============================================================================ escritura
def escribir():
    sm, rp = SALIDA / f"{NOMBRE}.SemanticModel", SALIDA / f"{NOMBRE}.Report"
    sm.mkdir(parents=True, exist_ok=True)
    rp.mkdir(parents=True, exist_ok=True)
    plataforma = lambda tipo: {
        "$schema": "https://developer.microsoft.com/json-schemas/fabric/gitIntegration/platformProperties/2.0.0/schema.json",
        "metadata": {"type": tipo, "displayName": NOMBRE},
        "config": {"version": "2.0", "logicalId": str(uuid.uuid5(uuid.NAMESPACE_DNS, NOMBRE + tipo))}}
    archivos = {
        SALIDA / f"{NOMBRE}.pbip": {"version": "1.0", "artifacts": [{"report": {"path": f"{NOMBRE}.Report"}}],
                                    "settings": {"enableAutoRecovery": True}},
        sm / ".platform": plataforma("SemanticModel"),
        sm / "definition.pbism": {"version": "1.0", "settings": {}},
        sm / "model.bim": modelo(),
        rp / ".platform": plataforma("Report"),
        rp / "definition.pbir": {"version": "1.0", "datasetReference": {
            "byPath": {"path": f"../{NOMBRE}.SemanticModel"}, "byConnection": None}},
        rp / "report.json": reporte(),
    }
    for ruta, contenido in archivos.items():
        ruta.write_text(json.dumps(contenido, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(f"Proyecto generado: {SALIDA / (NOMBRE + '.pbip')}")
    print(f"  3 tablas, {len(DAX)} medidas, {len(reporte()['sections'])} páginas")


if __name__ == "__main__":
    escribir()
