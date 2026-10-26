import { describirRuta } from './ruta';

describe('describirRuta', () => {
  it('traduce colecciones y campos, contando desde 1', () => {
    expect(describirRuta('usuarios[0].servicios.consultas[1].codDiagnosticoPrincipal')).toEqual([
      'Usuario 1',
      'Consulta 2',
      'Diagnóstico principal',
    ]);
  });

  it('conserva "servicios" cuando es el campo señalado', () => {
    expect(describirRuta('usuarios[2].servicios')).toEqual(['Usuario 3', 'Servicios']);
  });

  it('deja intactos los campos que no conoce', () => {
    expect(describirRuta('campoNuevo')).toEqual(['campoNuevo']);
  });
});
