import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', title: 'Validar RIPS', loadComponent: () => import('./validar/validar').then((m) => m.Validar) },
  { path: 'historial', title: 'Historial · RIPS Validator', loadComponent: () => import('./historial/historial').then((m) => m.Historial) },
  { path: 'validaciones/:id', title: 'Validación · RIPS Validator', loadComponent: () => import('./detalle/detalle').then((m) => m.Detalle) },
  { path: '**', redirectTo: '' },
];
