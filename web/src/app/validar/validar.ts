import { Component, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';

import { formatearTamano } from '../core/formato';
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

  protected readonly archivo = signal<Archivo | null>(null);
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

  protected probarEjemplo(nombre: string): void {
    this.empezar({ nombre, tamano: 'ejemplo' });
    this.api.ejemplo(nombre).subscribe({
      next: (contenido) => this.validar(contenido),
      error: () => this.fallar('No se pudo cargar el archivo de ejemplo.'),
    });
  }

  protected reiniciar(): void {
    this.archivo.set(null);
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
    this.enviar(this.api.validar(contenido));
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
