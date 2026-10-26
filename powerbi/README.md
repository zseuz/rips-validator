# Reporte de Power BI

Proyecto de Power BI en formato **PBIP** (texto, versionable en Git) sobre las validaciones de la API.

| Página | Qué muestra |
|---|---|
| Resumen | Validaciones, % de rechazo, % listas para enviar, errores y advertencias; estados, reglas y campos que más fallan; tendencia diaria |
| Reglas | Hallazgos y facturas afectadas por regla, grupo y severidad |
| Códigos | Diagnósticos (CIE-10), CUPS, municipios y países con más hallazgos, con su nombre oficial de SISPRO |
| Detalle | Un renglón por hallazgo: factura, usuario, campo, valor y mensaje |
| Cómo usar | Origen de los datos, actualización y definición de los indicadores |

## Modelo

Estrella con tres tablas en modo importación, leídas de la API con Power Query
(`Web.Contents` + `Csv.Document`) y 18 medidas DAX en `_Medidas`:

- **Validaciones** ← `/api/v1/exportaciones/validaciones.csv` (una fila por archivo validado)
- **Hallazgos** ← `/api/v1/exportaciones/hallazgos.csv` (una fila por hallazgo; relación con Validaciones y Reglas)
- **Reglas** ← `/api/v1/exportaciones/reglas.csv` (catálogo de reglas con grupo y severidad)

La dirección de la API es el parámetro `UrlApi` (por defecto `http://localhost:8080`).

## Usarlo

1. Con la API encendida, descarga el reporte desde **Historial › Descargar reporte** en el frontend
   (o desde `/api/v1/exportaciones/power-bi.zip`). El `.zip` trae `UrlApi` apuntando a esa API.
2. Descomprime y abre `RipsValidator.pbip` en Power BI Desktop.
3. Pulsa **Actualizar**. Si Power BI pide el tipo de acceso a la dirección web, elige **Anónimo**.

También puedes abrir directamente `powerbi/RipsValidator.pbip` de este repositorio: apunta a `localhost:8080`.

## Modificarlo

El proyecto lo genera [`generar_pbip.py`](generar_pbip.py) (sin dependencias): las medidas, columnas y
páginas están ahí. Después de cambiarlo:

```bash
python powerbi/generar_pbip.py
```

El CI comprueba que los archivos del repositorio coincidan con lo que genera el script.
