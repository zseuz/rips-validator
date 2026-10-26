import { DOCUMENT, Injectable, effect, inject, signal } from '@angular/core';

export type Tema = 'claro' | 'oscuro';

const CLAVE = 'rips-tema';

/**
 * Tema claro u oscuro. Arranca con el que se eligió la última vez o, si nunca se eligió, con el
 * del sistema; el script de index.html aplica el mismo valor antes de que cargue Angular para que
 * la página no parpadee.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  private readonly raiz = inject(DOCUMENT).documentElement;

  readonly tema = signal<Tema>(this.inicial());

  constructor() {
    effect(() => {
      this.raiz.dataset['tema'] = this.tema();
    });
  }

  /** Cambia de tema y lo recuerda para las próximas visitas. */
  alternar(): void {
    this.tema.update((tema) => (tema === 'oscuro' ? 'claro' : 'oscuro'));
    try {
      localStorage.setItem(CLAVE, this.tema());
    } catch {
      // Navegación privada o almacenamiento bloqueado: el tema dura solo esta visita.
    }
  }

  private inicial(): Tema {
    const guardado = this.raiz.dataset['tema'];
    if (guardado === 'claro' || guardado === 'oscuro') {
      return guardado;
    }
    return matchMedia('(prefers-color-scheme: dark)').matches ? 'oscuro' : 'claro';
  }
}
