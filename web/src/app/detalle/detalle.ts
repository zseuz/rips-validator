import { Component, inject, input, numberAttribute } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of, startWith, switchMap } from 'rxjs';

import { ResultadoValidacion } from '../core/modelos';
import { RipsApi } from '../core/rips-api';
import { Resultado } from '../resultado/resultado';

type Estado = { cargando: true } | { cargando: false; resultado?: ResultadoValidacion; error?: string };

/** Una validación del historial, abierta desde /validaciones/:id. */
@Component({
  selector: 'app-detalle',
  imports: [Resultado, RouterLink],
  template: `
    <a class="volver" routerLink="/historial">Volver al historial</a>
    @let e = estado();
    @if (e.cargando) {
      <p class="suave">Cargando…</p>
    } @else if (e.resultado) {
      <app-resultado [resultado]="e.resultado" />
    } @else {
      <p class="alerta" role="alert">{{ e.error }}</p>
    }
  `,
  styles: `
    :host { display: grid; gap: 1.25rem; }
    .volver { justify-self: start; color: var(--sello); font-weight: 500; }
    
    
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
