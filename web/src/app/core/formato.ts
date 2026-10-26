import { EstadoValidacion } from './modelos';

const FECHA = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

export function formatearFecha(iso: string): string {
  return FECHA.format(new Date(iso));
}

export function formatearTamano(bytes: number): string {
  return bytes < 1024 ? `${bytes} B` : bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

/** sello: lo que dice el sello estampado sobre el resultado, como en una ventanilla de radicación. */
export const ESTADOS: Record<EstadoValidacion, { etiqueta: string; sello: string; titulo: string; detalle: string }> = {
  VALIDO: {
    etiqueta: 'Válido',
    sello: 'Listo para radicar',
    titulo: 'Listo para enviar',
    detalle: 'No se encontraron errores ni advertencias.',
  },
  VALIDO_CON_ADVERTENCIAS: {
    etiqueta: 'Con advertencias',
    sello: 'Radicable con observaciones',
    titulo: 'Se puede enviar, pero revisa las advertencias',
    detalle: 'No hay errores que bloqueen el envío; las advertencias suelen terminar en glosas.',
  },
  RECHAZADO: {
    etiqueta: 'Rechazado',
    sello: 'Devuelto',
    titulo: 'Corrige los errores antes de enviarlo',
    detalle: 'El Ministerio devolvería este archivo sin generar el CUV.',
  },
};
