import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { ESTADOS, formatearFecha } from '../core/formato';
import { Pagina, ResumenValidacion } from '../core/modelos';
import { RipsApi } from '../core/rips-api';

@Component({
  selector: 'app-historial',
  imports: [RouterLink],
  templateUrl: './historial.html',
  styleUrl: './historial.scss',
})
export class Historial implements OnInit {
  private readonly api = inject(RipsApi);
  private readonly router = inject(Router);

  protected readonly pagina = signal<Pagina<ResumenValidacion> | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly estados = ESTADOS;
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
