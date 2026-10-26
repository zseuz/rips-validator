import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';

/**
 * Número que sube desde 0 hasta su valor al aparecer. Con "reducir movimiento" activado en el
 * sistema muestra el valor directamente.
 */
@Component({
  selector: 'app-contador',
  template: '{{ mostrado() }}',
  host: { class: 'tabular-nums' },
})
export class Contador {
  readonly valor = input.required<number>();
  readonly duracion = input(900);

  protected readonly mostrado = signal(0);
  private cuadro = 0;

  constructor() {
    inject(DestroyRef).onDestroy(() => cancelAnimationFrame(this.cuadro));
    effect(() => {
      const destino = this.valor();
      const reducir = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
      if (reducir || destino === 0 || typeof requestAnimationFrame !== 'function') {
        this.mostrado.set(destino);
        return;
      }
      cancelAnimationFrame(this.cuadro);
      const inicio = performance.now();
      const paso = (ahora: number) => {
        const t = Math.min(1, (ahora - inicio) / this.duracion());
        // ease-out: rápido al comienzo y suave al llegar.
        this.mostrado.set(Math.round(destino * (1 - Math.pow(1 - t, 3))));
        if (t < 1) {
          this.cuadro = requestAnimationFrame(paso);
        }
      };
      this.cuadro = requestAnimationFrame(paso);
    });
  }
}
