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
const TONOS: Record<EstadoValidacion, { banner: string; icono: string; insignia: string; punto: string; nombre: NombreIcono }> = {
  RECHAZADO: {
    banner: 'border-rose-500/20 bg-rose-500/10 shadow-rose-500/10 dark:bg-rose-500/10',
    icono: 'bg-gradient-to-br from-rose-500 to-rose-600 shadow-rose-500/40',
    insignia: 'border-rose-500/30 bg-rose-500/10 text-rose-700 shadow-rose-500/20 dark:text-rose-300',
    punto: 'bg-rose-500',
    nombre: 'error',
  },
  VALIDO_CON_ADVERTENCIAS: {
    banner: 'border-amber-500/20 bg-amber-500/10 shadow-amber-500/10 dark:bg-amber-500/10',
    icono: 'bg-gradient-to-br from-amber-400 to-amber-500 shadow-amber-500/40',
    insignia: 'border-amber-500/30 bg-amber-500/10 text-amber-700 shadow-amber-500/20 dark:text-amber-300',
    punto: 'bg-amber-500',
    nombre: 'advertencia',
  },
  VALIDO: {
    banner: 'border-emerald-500/20 bg-emerald-500/10 shadow-emerald-500/10 dark:bg-emerald-500/10',
    icono: 'bg-gradient-to-br from-emerald-500 to-emerald-600 shadow-emerald-500/40',
    insignia: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-700 shadow-emerald-500/20 dark:text-emerald-300',
    punto: 'bg-emerald-500',
    nombre: 'ok',
  },
};

interface Kpi {
  etiqueta: string;
  icono: NombreIcono;
  /** Las de errores y advertencias son de acción prioritaria: más contraste y borde brillante. */
  prioritaria: boolean;
  /** Clases de la tarjeta, del ícono y del número. */
  tarjeta: string;
  icon: string;
  cifra: string;
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
  /** Hallazgos desplegados en el acordeón, por posición en la respuesta de la API. */
  protected readonly abiertos = signal(new Set<number>());

  protected readonly estado = computed(() => ESTADOS[this.resultado().estado]);
  protected readonly tono = computed(() => TONOS[this.resultado().estado]);

  protected readonly kpis = computed<Kpi[]>(() => {
    const r = this.resultado();
    const neutra = {
      prioritaria: false,
      tarjeta: 'border-slate-200/70 bg-white/40 dark:border-slate-800/80 dark:bg-slate-900/30',
      icon: 'text-slate-400 dark:text-slate-500',
      cifra: 'text-slate-900 dark:text-white',
    };
    const apagada = { ...neutra, prioritaria: true };
    return [
      { etiqueta: 'Factura', icono: 'archivo-texto', ...neutra, texto: r.numFactura ?? 'Sin número' },
      { etiqueta: 'NIT entidad', icono: 'edificio', ...neutra, texto: r.numDocumentoIdObligado ?? 'Sin NIT' },
      { etiqueta: 'Usuarios', icono: 'usuarios', ...neutra, numero: r.totalUsuarios },
      { etiqueta: 'Servicios', icono: 'estetoscopio', ...neutra, numero: r.totalServicios },
      {
        etiqueta: 'Errores', icono: 'error', numero: r.totalErrores,
        ...(r.totalErrores > 0
          ? {
              prioritaria: true,
              tarjeta: 'border-rose-400/70 bg-gradient-to-br from-rose-500/20 via-rose-500/10 to-white/60 ring-1 ring-rose-400/40 shadow-[0_0_28px_-6px] shadow-rose-500/60 dark:from-rose-500/25 dark:via-rose-500/10 dark:to-slate-900/60',
              icon: 'text-rose-600 dark:text-rose-300',
              cifra: 'text-rose-600 dark:text-rose-300',
            }
          : apagada),
      },
      {
        etiqueta: 'Advertencias', icono: 'advertencia', numero: r.totalAdvertencias,
        ...(r.totalAdvertencias > 0
          ? {
              prioritaria: true,
              tarjeta: 'border-amber-400/70 bg-gradient-to-br from-amber-400/25 via-amber-400/10 to-white/60 ring-1 ring-amber-400/40 shadow-[0_0_28px_-6px] shadow-amber-500/60 dark:from-amber-400/20 dark:via-amber-400/10 dark:to-slate-900/60',
              icon: 'text-amber-600 dark:text-amber-300',
              cifra: 'text-amber-600 dark:text-amber-300',
            }
          : apagada),
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
    const titulos = new Map(this.porRegla().map((g) => [g.codigo, g.titulo]));
    return this.resultado()
      .hallazgos.filter(
        (h) =>
          (filtro === 'TODOS' || h.severidad === filtro) &&
          (regla === null || h.codigo === regla) &&
          (texto === '' || [h.codigo, titulos.get(h.codigo) ?? '', h.mensaje, h.ruta, h.valor ?? ''].some((campo) => campo.toLowerCase().includes(texto))),
      )
      .sort(enOrdenDelArchivo);
  });

  protected readonly migas = describirRuta;

  protected posicion(h: Hallazgo): number {
    return this.resultado().hallazgos.indexOf(h);
  }

  protected alternar(h: Hallazgo): void {
    const posicion = this.posicion(h);
    this.abiertos.update((abiertos) => {
      const nuevos = new Set(abiertos);
      if (!nuevos.delete(posicion)) {
        nuevos.add(posicion);
      }
      return nuevos;
    });
  }

  protected tituloRegla(codigo: string): string {
    return this.porRegla().find((g) => g.codigo === codigo)?.titulo ?? codigo;
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
