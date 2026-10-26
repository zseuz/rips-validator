import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { Icono } from './core/icono';
import { TemaService } from './core/tema';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icono],
  templateUrl: './app.html',
})
export class App {
  protected readonly tema = inject(TemaService);
}
