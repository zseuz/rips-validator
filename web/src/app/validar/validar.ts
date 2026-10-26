import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, forkJoin, of } from 'rxjs';

import { formatearFecha, formatearTamano } from '../core/formato';
import { Icono, NombreIcono } from '../core/icono';
import { ResultadoValidacion } from '../core/modelos';
import { RipsApi } from '../core/rips-api';
import { Resultado } from '../resultado/resultado';

const TAMANO_MAXIMO = 25 * 1024 * 1024;

type Campo = 'rips' | 'factura';

interface Archivo {
  nombre: string;
  tamano: string;
  contenido: Blob;
}

const EJEMPLOS = {
  correcto: { rips: 'rips-valido.json', factura: 'factura-FEV1001.xml' },
  errores: { rips: 'rips-con-errores.json', factura: 'factura-FEV1002.xml' },
} as const;

/**
 * Pantalla principal: un formulario de radicación con dos campos obligatorios, el RIPS en JSON y
 * el XML de su factura electrónica. Se valida cuando están los dos.
 */
@Component({
  selector: 'app-validar',
  imports: [Resultado, Icono],
  templateUrl: './validar.html',
})
export class Validar {
  private readonly api = inject(RipsApi);

  protected readonly campos: { id: Campo; nombre: string; ayuda: string; acepta: string; icono: NombreIcono }[] = [
    { id: 'rips', nombre: 'RIPS de la factura', ayuda: 'Archivo .json de la Resolución 2275 de 2023', acepta: '.json,application/json', icono: 'archivo-codigo' },
    { id: 'factura', nombre: 'Factura electrónica', ayuda: 'Archivo .xml de la DIAN (Invoice o AttachedDocument)', acepta: '.xml,application/xml,text/xml', icono: 'archivo-texto' },
  ];

  protected readonly revisiones: { titulo: string; texto: string; icono: NombreIcono; tono: string }[] = [
    {
      titulo: 'Códigos oficiales',
      texto: 'Cada código debe existir y estar habilitado en SISPRO: CIE-10, CUPS, municipios, prestadores del REPS y más.',
      icono: 'escudo',
      tono: 'bg-indigo-500/10 text-indigo-600 dark:text-indigo-300',
    },
    {
      titulo: 'Usuarios y servicios',
      texto: 'Consultas, procedimientos, urgencias, hospitalización, recién nacidos, medicamentos y otros servicios.',
      icono: 'usuarios',
      tono: 'bg-sky-500/10 text-sky-600 dark:text-sky-300',
    },
    {
      titulo: 'Coherencia clínica',
      texto: 'Diagnósticos que no aplican al sexo o a la edad, CUPS mal clasificados y egresos antes del ingreso.',
      icono: 'estetoscopio',
      tono: 'bg-violet-500/10 text-violet-600 dark:text-violet-300',
    },
    {
      titulo: 'Cruce con la factura',
      texto: 'Número de factura, NIT del emisor, valor total y fechas del XML frente a lo que dice el RIPS.',
      icono: 'archivo-texto',
      tono: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-300',
    },
  ];

  protected readonly rips = signal<Archivo | null>(null);
  protected readonly factura = signal<Archivo | null>(null);
  protected readonly arrastrando = signal<Campo | null>(null);
  protected readonly validando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly resultado = signal<ResultadoValidacion | null>(null);

  protected readonly faltan = computed(() => {
    const faltan = [];
    if (!this.rips()) faltan.push('el RIPS');
    if (!this.factura()) faltan.push('la factura');
    return faltan.join(' y ');
  });

  /** Resumen de las tablas de SISPRO que tiene cargadas la API. */
  private readonly tablas = toSignal(this.api.tablas$.pipe(catchError(() => of([]))), { initialValue: [] });
  protected readonly resumenTablas = computed(() => {
    const tablas = this.tablas();
    if (tablas.length === 0) {
      return null;
    }
    const registros = (nombre: string) => (tablas.find((t) => t.tabla === nombre)?.registros ?? 0).toLocaleString('es-CO');
    const fechas = tablas.map((t) => t.descargada).filter((f): f is string => f !== null).sort();
    return {
      cantidad: tablas.length,
      cie10: registros('CIE10'),
      cups: registros('CUPSRips'),
      reps: registros('IPSCodHabilitacion'),
      fecha: fechas.length > 0 ? formatearFecha(fechas[fechas.length - 1]) : null,
    };
  });

  protected alSoltar(campo: Campo, evento: DragEvent): void {
    evento.preventDefault();
    this.arrastrando.set(null);
    const archivo = evento.dataTransfer?.files[0];
    if (archivo) {
      this.cargar(campo, archivo);
    }
  }

  protected alElegir(campo: Campo, evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    entrada.value = ''; // permite volver a elegir el mismo archivo después de corregirlo
    if (archivo) {
      this.cargar(campo, archivo);
    }
  }

  protected quitar(campo: Campo): void {
    (campo === 'rips' ? this.rips : this.factura).set(null);
    this.error.set(null);
  }

  protected async validar(): Promise<void> {
    const rips = this.rips();
    const factura = this.factura();
    if (!rips || !factura) {
      return;
    }
    const json = await rips.contenido.text();
    try {
      JSON.parse(json);
    } catch (e) {
      this.error.set(`${rips.nombre} no es un JSON válido: ${(e as Error).message}`);
      return;
    }
    this.error.set(null);
    this.validando.set(true);
    this.api.validarConFactura(json, factura.contenido, factura.nombre).subscribe({
      next: (resultado) => {
        this.validando.set(false);
        this.resultado.set(resultado);
      },
      error: (e: Error) => {
        this.validando.set(false);
        this.error.set(e.message);
      },
    });
  }

  protected usarEjemplo(cual: keyof typeof EJEMPLOS): void {
    const ejemplo = EJEMPLOS[cual];
    forkJoin([this.api.ejemplo(ejemplo.rips), this.api.ejemplo(ejemplo.factura)]).subscribe({
      next: ([rips, factura]) => {
        this.rips.set(archivo(ejemplo.rips, new Blob([rips], { type: 'application/json' })));
        this.factura.set(archivo(ejemplo.factura, new Blob([factura], { type: 'application/xml' })));
        this.validar();
      },
      error: () => this.error.set('No se pudieron cargar los archivos de ejemplo.'),
    });
  }

  protected validarOtros(): void {
    this.rips.set(null);
    this.factura.set(null);
    this.resultado.set(null);
    this.error.set(null);
  }

  private cargar(campo: Campo, file: File): void {
    const extension = campo === 'rips' ? '.json' : '.xml';
    if (!file.name.toLowerCase().endsWith(extension)) {
      this.error.set(
        campo === 'rips'
          ? `${file.name} no es un .json. El RIPS de la Resolución 2275 de 2023 se reporta en JSON.`
          : `${file.name} no es un .xml. La factura electrónica es el XML que entrega la DIAN.`,
      );
      return;
    }
    if (file.size > TAMANO_MAXIMO) {
      this.error.set(`${file.name} supera el máximo de ${formatearTamano(TAMANO_MAXIMO)}.`);
      return;
    }
    this.error.set(null);
    (campo === 'rips' ? this.rips : this.factura).set(archivo(file.name, file));
  }
}

function archivo(nombre: string, contenido: Blob): Archivo {
  return { nombre, tamano: formatearTamano(contenido.size), contenido };
}
