import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Observable, catchError, forkJoin, of } from 'rxjs';

import { formatearFecha, formatearTamano } from '../core/formato';
import { ResultadoValidacion } from '../core/modelos';
import { RipsApi } from '../core/rips-api';
import { Resultado } from '../resultado/resultado';

const TAMANO_MAXIMO = 25 * 1024 * 1024;

interface Archivo {
  nombre: string;
  tamano: string;
}

/** Pantalla principal: se arrastra o elige un archivo y se valida en el acto. */
@Component({
  selector: 'app-validar',
  imports: [Resultado],
  templateUrl: './validar.html',
  styleUrl: './validar.scss',
})
export class Validar {
  private readonly api = inject(RipsApi);

  /** Resumen de las tablas de SISPRO que tiene cargadas la API, para mostrarlo bajo las tarjetas. */
  protected readonly tablas = toSignal(this.api.tablas$.pipe(catchError(() => of([]))), { initialValue: [] });
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
      fecha: fechas.length > 0 ? formatearFecha(fechas[fechas.length - 1]) : null,
    };
  });

  protected readonly archivo = signal<Archivo | null>(null);
  /** XML de la factura electrónica, opcional: si está, la validación también cruza RIPS y factura. */
  protected readonly factura = signal<{ nombre: string; contenido: Blob } | null>(null);
  protected readonly validando = signal(false);
  protected readonly arrastrando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly resultado = signal<ResultadoValidacion | null>(null);

  protected alSoltar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastrando.set(false);
    const archivo = evento.dataTransfer?.files[0];
    if (archivo) {
      this.cargar(archivo);
    }
  }

  protected alElegir(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const archivo = campo.files?.[0];
    campo.value = ''; // permite volver a elegir el mismo archivo después de corregirlo
    if (archivo) {
      this.cargar(archivo);
    }
  }

  protected alElegirFactura(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const archivo = campo.files?.[0];
    campo.value = '';
    if (archivo) {
      this.factura.set({ nombre: archivo.name, contenido: archivo });
    }
  }

  protected probarEjemplo(nombre: string, conFactura = false): void {
    this.empezar({ nombre, tamano: 'ejemplo' });
    const xml = conFactura ? this.api.ejemplo('factura-FEV1001.xml') : of(null);
    forkJoin([this.api.ejemplo(nombre), xml]).subscribe({
      next: ([contenido, factura]) => {
        if (factura !== null) {
          this.factura.set({ nombre: 'factura-FEV1001.xml', contenido: new Blob([factura], { type: 'application/xml' }) });
        }
        this.validar(contenido);
      },
      error: () => this.fallar('No se pudo cargar el archivo de ejemplo.'),
    });
  }

  protected reiniciar(): void {
    this.archivo.set(null);
    this.factura.set(null);
    this.resultado.set(null);
    this.error.set(null);
  }

  private async cargar(archivo: File): Promise<void> {
    this.empezar({ nombre: archivo.name, tamano: formatearTamano(archivo.size) });
    if (!archivo.name.toLowerCase().endsWith('.json')) {
      this.fallar('El archivo debe ser .json. Los RIPS de la Resolución 2275 de 2023 se reportan en JSON.');
      return;
    }
    if (archivo.size > TAMANO_MAXIMO) {
      this.fallar(`El archivo supera ${formatearTamano(TAMANO_MAXIMO)}.`);
      return;
    }
    this.validar(await archivo.text());
  }

  /** Revisa en el navegador que sea JSON para dar un error claro sin llamar a la API. */
  private validar(contenido: string): void {
    try {
      JSON.parse(contenido);
    } catch (e) {
      this.fallar(`El archivo no es un JSON válido: ${(e as Error).message}`);
      return;
    }
    const factura = this.factura();
    this.enviar(factura ? this.api.validarConFactura(contenido, factura.contenido, factura.nombre) : this.api.validar(contenido));
  }

  private enviar(peticion: Observable<ResultadoValidacion>): void {
    peticion.subscribe({
      next: (resultado) => {
        this.validando.set(false);
        this.resultado.set(resultado);
      },
      error: (e: Error) => this.fallar(e.message),
    });
  }

  private empezar(archivo: Archivo): void {
    this.archivo.set(archivo);
    this.resultado.set(null);
    this.error.set(null);
    this.validando.set(true);
  }

  private fallar(mensaje: string): void {
    this.validando.set(false);
    this.error.set(mensaje);
  }
}
