import { Component, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { ESTADOS, formatearFecha } from '../core/formato';
import { Explicacion, Hallazgo, ResultadoValidacion, Severidad } from '../core/modelos';
import { RipsApi } from '../core/rips-api';
import { describirRuta } from '../core/ruta';

type Filtro = 'TODOS' | Severidad;

interface EstadoExplicacion {
  cargando: boolean;
  explicacion?: Explicacion;
  error?: string;
}

/** Ordena por posición en el JSON (usuarios[2] antes que usuarios[10]); la API los agrupa por regla. */
const comparadorRutas = new Intl.Collator('es', { numeric: true });
const enOrdenDelArchivo = (a: Hallazgo, b: Hallazgo) => comparadorRutas.compare(a.ruta, b.ruta);

interface ResumenRegla {
  codigo: string;
  titulo: string;
  severidad: Severidad;
  cantidad: number;
}

/** Muestra el resultado de una validación: estado, totales y la lista filtrable de hallazgos. */
@Component({
  selector: 'app-resultado',
  templateUrl: './resultado.html',
  styleUrl: './resultado.scss',
})
export class Resultado {
  readonly resultado = input.required<ResultadoValidacion>();

  private readonly api = inject(RipsApi);
  private readonly reglas = toSignal(this.api.reglas$, { initialValue: [] });
  protected readonly ia = toSignal(this.api.ia$, { initialValue: { disponible: false, modelo: null } });

  /** Explicaciones pedidas, por posición del hallazgo en la respuesta de la API. */
  protected readonly explicaciones = signal(new Map<number, EstadoExplicacion>());

  protected readonly filtro = signal<Filtro>('TODOS');
  protected readonly regla = signal<string | null>(null);
  protected readonly busqueda = signal('');

  protected readonly estado = computed(() => ESTADOS[this.resultado().estado]);
  protected readonly fecha = computed(() => formatearFecha(this.resultado().fechaValidacion));

  /** Hallazgos agrupados por regla, de la más frecuente a la menos. */
  protected readonly porRegla = computed<ResumenRegla[]>(() => {
    const titulos = new Map(this.reglas().map((r) => [r.codigo, r.titulo]));
    const grupos = new Map<string, ResumenRegla>();
    for (const h of this.resultado().hallazgos) {
      const grupo = grupos.get(h.codigo) ?? { codigo: h.codigo, titulo: titulos.get(h.codigo) ?? h.codigo, severidad: h.severidad, cantidad: 0 };
      grupo.cantidad++;
      grupos.set(h.codigo, grupo);
    }
    return [...grupos.values()].sort((a, b) => b.cantidad - a.cantidad || a.codigo.localeCompare(b.codigo));
  });

  protected readonly visibles = computed(() => {
    const filtro = this.filtro();
    const regla = this.regla();
    const texto = this.busqueda().trim().toLowerCase();
    return this.resultado()
      .hallazgos.filter(
        (h) =>
          (filtro === 'TODOS' || h.severidad === filtro) &&
          (regla === null || h.codigo === regla) &&
          (texto === '' || [h.ruta, h.mensaje, h.valor ?? '', h.codigo].some((campo) => campo.toLowerCase().includes(texto))),
      )
      .sort(enOrdenDelArchivo);
  });

  protected readonly migas = describirRuta;

  protected posicion(h: Hallazgo): number {
    return this.resultado().hallazgos.indexOf(h);
  }

  protected explicar(h: Hallazgo): void {
    const posicion = this.posicion(h);
    const fijar = (estado: EstadoExplicacion) => this.explicaciones.update((m) => new Map(m).set(posicion, estado));
    fijar({ cargando: true });
    this.api.explicar(this.resultado().id, posicion).subscribe({
      next: (explicacion) => fijar({ cargando: false, explicacion }),
      error: (e: Error) => fijar({ cargando: false, error: e.message }),
    });
  }

  protected alternarRegla(codigo: string): void {
    this.regla.update((actual) => (actual === codigo ? null : codigo));
  }

  protected limpiarFiltros(): void {
    this.filtro.set('TODOS');
    this.regla.set(null);
    this.busqueda.set('');
  }

  /** CSV con BOM para que Excel respete las tildes. */
  protected descargarCsv(): void {
    const r = this.resultado();
    const columnas: (keyof Hallazgo)[] = ['codigo', 'severidad', 'ruta', 'mensaje', 'valor'];
    const celda = (v: string | null) => `"${(v ?? '').replaceAll('"', '""')}"`;
    const filas = [columnas.join(','), ...r.hallazgos.map((h) => columnas.map((c) => celda(h[c])).join(','))];
    const url = URL.createObjectURL(new Blob(['﻿' + filas.join('\r\n')], { type: 'text/csv;charset=utf-8' }));
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = `hallazgos-${r.numFactura ?? r.id}.csv`;
    enlace.click();
    URL.revokeObjectURL(url);
  }
}
