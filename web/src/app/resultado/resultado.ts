import { Component, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { Contador } from '../core/contador';
import { ESTADOS, formatearFecha } from '../core/formato';
import { Icono, NombreIcono } from '../core/icono';
import { EstadoValidacion, Explicacion, Hallazgo, ResultadoValidacion, Severidad } from '../core/modelos';
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

/**
 * Colores de cada estado. Las clases van completas (no armadas por partes) para que Tailwind las
 * encuentre al compilar.
 */
const TONOS: Record<EstadoValidacion, { banner: string; icono: string; halo: string; chip: string; titulo: string; nombre: NombreIcono }> = {
  RECHAZADO: {
    banner: 'border-rose-500/20 bg-rose-500/10 shadow-rose-500/10 dark:bg-rose-500/10',
    icono: 'bg-gradient-to-br from-rose-500 to-rose-600 shadow-rose-500/40',
    halo: 'bg-rose-500',
    chip: 'bg-rose-500/15 text-rose-700 ring-rose-500/30 dark:text-rose-300',
    titulo: 'text-rose-700 dark:text-rose-300',
    nombre: 'error',
  },
  VALIDO_CON_ADVERTENCIAS: {
    banner: 'border-amber-500/20 bg-amber-500/10 shadow-amber-500/10 dark:bg-amber-500/10',
    icono: 'bg-gradient-to-br from-amber-400 to-amber-500 shadow-amber-500/40',
    halo: 'bg-amber-500',
    chip: 'bg-amber-500/15 text-amber-700 ring-amber-500/30 dark:text-amber-300',
    titulo: 'text-amber-700 dark:text-amber-300',
    nombre: 'advertencia',
  },
  VALIDO: {
    banner: 'border-emerald-500/20 bg-emerald-500/10 shadow-emerald-500/10 dark:bg-emerald-500/10',
    icono: 'bg-gradient-to-br from-emerald-500 to-emerald-600 shadow-emerald-500/40',
    halo: 'bg-emerald-500',
    chip: 'bg-emerald-500/15 text-emerald-700 ring-emerald-500/30 dark:text-emerald-300',
    titulo: 'text-emerald-700 dark:text-emerald-300',
    nombre: 'ok',
  },
};

interface Kpi {
  etiqueta: string;
  icono: NombreIcono;
  /** Clases del cuadro del ícono y, si aplica, del fondo de la tarjeta. */
  tono: string;
  tarjeta: string;
  numero?: number;
  texto?: string;
}

interface ResumenRegla {
  codigo: string;
  titulo: string;
  severidad: Severidad;
  cantidad: number;
}

/** Muestra el resultado de una validación: estado, totales y la lista filtrable de hallazgos. */
@Component({
  selector: 'app-resultado',
  imports: [Icono, Contador],
  templateUrl: './resultado.html',
})
export class Resultado {
  readonly resultado = input.required<ResultadoValidacion>();

  private readonly api = inject(RipsApi);
  private readonly reglas = toSignal(this.api.reglas$, { initialValue: [] });
  protected readonly ia = toSignal(this.api.ia$, { initialValue: { disponible: false, modelo: null } });

  /** Explicaciones pedidas, por posición del hallazgo en la respuesta de la API. */
  protected readonly explicaciones = signal(new Map<number, EstadoExplicacion>());

  protected readonly filtros: { valor: Filtro; etiqueta: string }[] = [
    { valor: 'TODOS', etiqueta: 'Todos' },
    { valor: 'ERROR', etiqueta: 'Errores' },
    { valor: 'ADVERTENCIA', etiqueta: 'Advertencias' },
  ];
  protected readonly filtro = signal<Filtro>('TODOS');
  protected readonly regla = signal<string | null>(null);
  protected readonly busqueda = signal('');

  protected readonly estado = computed(() => ESTADOS[this.resultado().estado]);
  protected readonly tono = computed(() => TONOS[this.resultado().estado]);

  protected readonly kpis = computed<Kpi[]>(() => {
    const r = this.resultado();
    const neutra = 'border-slate-200/80 bg-white/70 dark:border-slate-800 dark:bg-slate-900/60';
    return [
      { etiqueta: 'Factura', icono: 'archivo-texto', tono: 'bg-indigo-500/10 text-indigo-600 dark:text-indigo-300', tarjeta: neutra, texto: r.numFactura ?? 'Sin número' },
      { etiqueta: 'Obligado', icono: 'edificio', tono: 'bg-indigo-500/10 text-indigo-600 dark:text-indigo-300', tarjeta: neutra, texto: r.numDocumentoIdObligado ?? 'Sin NIT' },
      { etiqueta: 'Usuarios', icono: 'usuarios', tono: 'bg-sky-500/10 text-sky-600 dark:text-sky-300', tarjeta: neutra, numero: r.totalUsuarios },
      { etiqueta: 'Servicios', icono: 'estetoscopio', tono: 'bg-violet-500/10 text-violet-600 dark:text-violet-300', tarjeta: neutra, numero: r.totalServicios },
      {
        etiqueta: 'Errores', icono: 'error', numero: r.totalErrores,
        tono: 'bg-rose-500/15 text-rose-600 dark:text-rose-300',
        tarjeta: r.totalErrores > 0 ? 'border-rose-500/25 bg-rose-500/10 shadow-rose-500/10 dark:bg-rose-500/10' : neutra,
      },
      {
        etiqueta: 'Advertencias', icono: 'advertencia', numero: r.totalAdvertencias,
        tono: 'bg-amber-500/15 text-amber-600 dark:text-amber-300',
        tarjeta: r.totalAdvertencias > 0 ? 'border-amber-500/25 bg-amber-500/10 shadow-amber-500/10 dark:bg-amber-500/10' : neutra,
      },
    ];
  });
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
