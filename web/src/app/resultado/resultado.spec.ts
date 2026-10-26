import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResultadoValidacion } from '../core/modelos';
import { Resultado } from './resultado';

const RESULTADO: ResultadoValidacion = {
  id: 7,
  numFactura: 'FEV1002',
  numDocumentoIdObligado: '900123456',
  estado: 'RECHAZADO',
  fechaValidacion: '2026-10-26T16:00:00Z',
  totalUsuarios: 2,
  totalServicios: 1,
  totalErrores: 2,
  totalAdvertencias: 1,
  hallazgos: [
    { codigo: 'RV013', severidad: 'ADVERTENCIA', ruta: 'usuarios[10].servicios', mensaje: 'Sin servicios', valor: null },
    { codigo: 'RV041', severidad: 'ERROR', ruta: 'usuarios[2].servicios.consultas[0].valorPagoModerador', mensaje: 'Moderador alto', valor: '52000' },
    { codigo: 'RV041', severidad: 'ERROR', ruta: 'usuarios[0].servicios.consultas[1].valorPagoModerador', mensaje: 'Moderador alto', valor: '60000' },
  ],
};

describe('Resultado', () => {
  let fixture: ComponentFixture<Resultado>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [Resultado],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(Resultado);
    fixture.componentRef.setInput('resultado', RESULTADO);
    TestBed.inject(HttpTestingController)
      .expectOne('/api/v1/reglas')
      .flush([{ codigo: 'RV041', severidad: 'ERROR', titulo: 'Pago moderador mayor que el valor del servicio' }]);
    await fixture.whenStable();
  });

  const codigosVisibles = () =>
    [...(fixture.nativeElement as HTMLElement).querySelectorAll('.hallazgo')].map((li) => li.querySelector('code:last-child')?.textContent);

  it('muestra los hallazgos en el orden del archivo', () => {
    expect(codigosVisibles()).toEqual([
      'usuarios[0].servicios.consultas[1].valorPagoModerador',
      'usuarios[2].servicios.consultas[0].valorPagoModerador',
      'usuarios[10].servicios',
    ]);
  });

  it('agrupa por regla con el título del catálogo', () => {
    const chips = [...(fixture.nativeElement as HTMLElement).querySelectorAll('.regla')].map((b) => b.getAttribute('aria-label'));
    expect(chips[0]).toBe('RV041, Pago moderador mayor que el valor del servicio: 2 hallazgos');
  });

  it('filtra por severidad', async () => {
    const botones = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.segmentos button');
    botones[2].click();
    await fixture.whenStable();
    expect(codigosVisibles()).toEqual(['usuarios[10].servicios']);
  });
});
