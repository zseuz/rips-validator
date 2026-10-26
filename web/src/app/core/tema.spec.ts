import { TestBed } from '@angular/core/testing';

import { TemaService } from './tema';

describe('TemaService', () => {
  beforeEach(() => {
    localStorage.clear();
    delete document.documentElement.dataset['tema'];
  });

  it('respeta el tema que index.html ya aplicó desde lo guardado', () => {
    document.documentElement.dataset['tema'] = 'oscuro';
    expect(TestBed.inject(TemaService).tema()).toBe('oscuro');
  });

  it('alterna, marca <html> y recuerda la elección', async () => {
    document.documentElement.dataset['tema'] = 'oscuro';
    const servicio = TestBed.inject(TemaService);

    servicio.alternar();
    TestBed.tick();

    expect(servicio.tema()).toBe('claro');
    expect(document.documentElement.dataset['tema']).toBe('claro');
    expect(localStorage.getItem('rips-tema')).toBe('claro');
  });
});
