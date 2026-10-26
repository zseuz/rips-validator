import { Component, ElementRef, effect, inject, input } from '@angular/core';
import {
  Building2,
  ChartColumn,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  CircleCheck,
  CircleX,
  Download,
  FileCode2,
  FileText,
  History,
  Moon,
  Search,
  ShieldCheck,
  Sparkles,
  Stethoscope,
  Sun,
  TriangleAlert,
  Upload,
  Users,
  X,
  createElement,
  type IconNode,
} from 'lucide';

/** Íconos de Lucide que usa la aplicación, por un nombre propio. */
const ICONOS = {
  'archivo-codigo': FileCode2,
  'archivo-texto': FileText,
  'abajo': ChevronDown,
  'atras': ChevronLeft,
  'buscar': Search,
  'cerrar': X,
  'chispas': Sparkles,
  'descargar': Download,
  'edificio': Building2,
  'error': CircleX,
  'escudo': ShieldCheck,
  'estetoscopio': Stethoscope,
  'grafico': ChartColumn,
  'historial': History,
  'luna': Moon,
  'ok': CircleCheck,
  'siguiente': ChevronRight,
  'sol': Sun,
  'subir': Upload,
  'usuarios': Users,
  'advertencia': TriangleAlert,
} satisfies Record<string, IconNode>;

export type NombreIcono = keyof typeof ICONOS;

/**
 * Ícono de Lucide como SVG en línea. Se usa el paquete base de Lucide (no lucide-angular, que aún
 * no declara compatibilidad con Angular 22) y se dibuja con su propio createElement.
 */
@Component({
  selector: 'app-icono',
  template: '',
  host: { class: 'inline-flex shrink-0', 'aria-hidden': 'true' },
})
export class Icono {
  readonly nombre = input.required<NombreIcono>();
  readonly tamano = input(20);
  readonly trazo = input(1.75);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  constructor() {
    effect(() => {
      const svg = createElement(ICONOS[this.nombre()], {
        width: this.tamano(),
        height: this.tamano(),
        'stroke-width': this.trazo(),
      });
      this.host.nativeElement.replaceChildren(svg);
    });
  }
}
