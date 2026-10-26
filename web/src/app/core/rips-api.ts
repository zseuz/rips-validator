import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, shareReplay, throwError } from 'rxjs';

import { Pagina, Regla, ResultadoValidacion, ResumenValidacion, TablaReferencia } from './modelos';

const BASE = '/api/v1';

@Injectable({ providedIn: 'root' })
export class RipsApi {
  private readonly http = inject(HttpClient);

  /** El catálogo no cambia mientras corre la API: se pide una sola vez. */
  readonly reglas$: Observable<Regla[]> = this.http.get<Regla[]>(`${BASE}/reglas`).pipe(shareReplay(1));

  readonly tablas$: Observable<TablaReferencia[]> = this.http.get<TablaReferencia[]>(`${BASE}/tablas`).pipe(shareReplay(1));

  /** Envía el archivo tal cual, sin reinterpretarlo, para que la API vea exactamente lo que se subió. */
  validar(json: string): Observable<ResultadoValidacion> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http
      .post<ResultadoValidacion>(`${BASE}/validaciones`, json, { headers })
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
  const mensaje =
    error.status === 0
      ? 'No se pudo conectar con la API. Verifica que esté corriendo en el puerto 8080.'
      : error.status === 400
        ? 'El archivo no tiene la estructura de un RIPS en JSON (Resolución 2275 de 2023).'
        : error.status === 404
          ? 'La validación no existe.'
          : `La API respondió con un error (${error.status}).`;
  return throwError(() => new Error(mensaje));
}
