import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { ESTADOS, formatearFecha } from '../core/formato';
import { Icono } from '../core/icono';
import { EstadoValidacion, Pagina, ResumenValidacion } from '../core/modelos';
import { RipsApi } from '../core/rips-api';

@Component({
  selector: 'app-historial',
  imports: [RouterLink, Icono],
  templateUrl: './historial.html',
})
export class Historial implements OnInit {
  private readonly api = inject(RipsApi);
  private readonly router = inject(Router);

  protected readonly pagina = signal<Pagina<ResumenValidacion> | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly estados = ESTADOS;
  protected readonly insignias: Record<EstadoValidacion, string> = {
    VALIDO: 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-300',
    VALIDO_CON_ADVERTENCIAS: 'bg-amber-500/10 text-amber-700 dark:text-amber-300',
    RECHAZADO: 'bg-rose-500/10 text-rose-700 dark:text-rose-300',
  };
  protected readonly fecha = formatearFecha;

  ngOnInit(): void {
    this.cargar(0);
  }

  protected cargar(numero: number): void {
    this.api.historial(numero).subscribe({
      next: (pagina) => this.pagina.set(pagina),
      error: (e: Error) => this.error.set(e.message),
    });
  }

  protected abrir(id: number): void {
    this.router.navigate(['/validaciones', id]);
  }
}
