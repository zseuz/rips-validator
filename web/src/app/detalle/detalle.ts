import { Component, inject, input, numberAttribute } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of, startWith, switchMap } from 'rxjs';

import { ResultadoValidacion } from '../core/modelos';
import { RipsApi } from '../core/rips-api';
import { Icono } from '../core/icono';
import { Resultado } from '../resultado/resultado';

type Estado = { cargando: true } | { cargando: false; resultado?: ResultadoValidacion; error?: string };

/** Una validación del historial, abierta desde /validaciones/:id. */
@Component({
  selector: 'app-detalle',
  imports: [Resultado, RouterLink, Icono],
  template: `
    <a routerLink="/historial"
       class="mb-5 inline-flex items-center gap-1 rounded-lg px-2 py-1 text-sm font-semibold text-indigo-600 transition hover:bg-indigo-500/10 dark:text-indigo-300">
      <app-icono nombre="atras" [tamano]="16" />
      Volver al historial
    </a>
    @let e = estado();
    @if (e.cargando) {
      <div class="h-40 animate-pulse rounded-2xl bg-slate-200/70 dark:bg-slate-800/70"></div>
    } @else if (e.resultado) {
      <app-resultado [resultado]="e.resultado" />
    } @else {
      <p class="rounded-xl border border-rose-500/20 bg-rose-500/10 px-4 py-3 text-rose-700 dark:text-rose-300" role="alert">{{ e.error }}</p>
    }
  `,
})
export class Detalle {
  private readonly api = inject(RipsApi);

  /** Llega desde el parámetro :id de la ruta. */
  readonly id = input.required({ transform: numberAttribute });

  protected readonly estado = toSignal(
    toObservable(this.id).pipe(
      switchMap((id) =>
        this.api.buscar(id).pipe(
          map((resultado): Estado => ({ cargando: false, resultado })),
          catchError((e: Error) => of<Estado>({ cargando: false, error: e.message })),
          startWith<Estado>({ cargando: true }),
        ),
      ),
    ),
    { initialValue: { cargando: true } as Estado },
  );
}
