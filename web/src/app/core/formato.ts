import { EstadoValidacion } from './modelos';

const FECHA = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

export function formatearFecha(iso: string): string {
  return FECHA.format(new Date(iso));
}

export function formatearTamano(bytes: number): string {
  return bytes < 1024 ? `${bytes} B` : bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

/** insignia: el texto del distintivo del banner de estado (y del historial, en versión corta: etiqueta). */
export const ESTADOS: Record<EstadoValidacion, { etiqueta: string; insignia: string; titulo: string; detalle: string }> = {
  VALIDO: {
    etiqueta: 'Válido',
    insignia: 'Listo para radicar',
    titulo: 'Validación aprobada',
    detalle: 'Listo para enviar. No se encontraron errores ni advertencias.',
  },
  VALIDO_CON_ADVERTENCIAS: {
    etiqueta: 'Con advertencias',
    insignia: 'Con observaciones',
    titulo: 'Validación con observaciones',
    detalle: 'Se puede enviar, pero revisa las advertencias: no bloquean el envío y suelen terminar en glosas.',
  },
  RECHAZADO: {
    etiqueta: 'Rechazado',
    insignia: 'Devuelto',
    titulo: 'Validación rechazada',
    detalle: 'Corrige los errores antes de enviarlo. El Ministerio devolvería este archivo sin generar el CUV.',
  },
};
