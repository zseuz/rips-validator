import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, shareReplay, throwError } from 'rxjs';

import { EstadoIa, Explicacion, Pagina, Regla, ResultadoValidacion, ResumenValidacion, TablaReferencia } from './modelos';

const BASE = '/api/v1';

@Injectable({ providedIn: 'root' })
export class RipsApi {
  private readonly http = inject(HttpClient);

  /** El catálogo no cambia mientras corre la API: se pide una sola vez. */
  readonly reglas$: Observable<Regla[]> = this.http.get<Regla[]>(`${BASE}/reglas`).pipe(shareReplay(1));

  readonly tablas$: Observable<TablaReferencia[]> = this.http.get<TablaReferencia[]>(`${BASE}/tablas`).pipe(shareReplay(1));

  /** Si la API tiene clave de Claude; sin ella no se muestra el botón "Explicar". */
  readonly ia$: Observable<EstadoIa> = this.http
    .get<EstadoIa>(`${BASE}/ia`)
    .pipe(catchError(() => of({ disponible: false, modelo: null })), shareReplay(1));

  /** posicion: índice del hallazgo en la lista que devolvió la API (no en la lista ordenada en pantalla). */
  explicar(validacion: number, posicion: number): Observable<Explicacion> {
    return this.http
      .post<Explicacion>(`${BASE}/validaciones/${validacion}/hallazgos/${posicion}/explicacion`, null)
      .pipe(catchError(traducirError));
  }

  /** Envía el archivo tal cual, sin reinterpretarlo, para que la API vea exactamente lo que se subió. */
  validar(json: string): Observable<ResultadoValidacion> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http
      .post<ResultadoValidacion>(`${BASE}/validaciones`, json, { headers })
      .pipe(catchError(traducirError));
  }

  /** Valida el RIPS y lo cruza con el XML de su factura electrónica. */
  validarConFactura(json: string, factura: Blob, nombreFactura: string): Observable<ResultadoValidacion> {
    const formulario = new FormData();
    formulario.append('rips', new Blob([json], { type: 'application/json' }), 'rips.json');
    formulario.append('factura', factura, nombreFactura);
    return this.http
      .post<ResultadoValidacion>(`${BASE}/validaciones/con-factura`, formulario)
      .pipe(catchError(traducirError));
  }

  buscar(id: number): Observable<ResultadoValidacion> {
    return this.http.get<ResultadoValidacion>(`${BASE}/validaciones/${id}`).pipe(catchError(traducirError));
  }

  historial(pagina: number, tamano = 15): Observable<Pagina<ResumenValidacion>> {
    return this.http
      .get<Pagina<ResumenValidacion>>(`${BASE}/validaciones`, { params: { page: pagina, size: tamano } })
      .pipe(catchError(traducirError));
  }

  /** Archivos de muestra de /ejemplos, servidos como estáticos por el frontend. */
  ejemplo(nombre: string): Observable<string> {
    return this.http.get(`ejemplos/${nombre}`, { responseType: 'text' });
  }
}

function traducirError(error: HttpErrorResponse): Observable<never> {
  // Para la factura XML la API explica qué no pudo leer (ProblemDetail.detail).
  const detalle: string | undefined = error.error?.detail;
  const mensaje =
    error.status === 0
      ? 'No se pudo conectar con la API. Verifica que esté corriendo en el puerto 8080.'
      : error.status === 400 && detalle
        ? detalle
        : error.status === 400
        ? 'El archivo no tiene la estructura de un RIPS en JSON (Resolución 2275 de 2023).'
        : error.status >= 500 && detalle
          ? detalle
        : error.status === 404
          ? 'La validación no existe.'
          : error.status === 413
            ? 'El archivo supera el tamaño máximo que acepta la API.'
            : error.status === 429
              ? 'Demasiadas validaciones seguidas. Espera un minuto e inténtalo de nuevo.'
              : `La API respondió con un error (${error.status}).`;
  return throwError(() => new Error(mensaje));
}
