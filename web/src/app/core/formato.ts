import { EstadoValidacion } from './modelos';

const FECHA = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

export function formatearFecha(iso: string): string {
  return FECHA.format(new Date(iso));
}

export function formatearTamano(bytes: number): string {
  return bytes < 1024 ? `${bytes} B` : bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export const ESTADOS: Record<EstadoValidacion, { etiqueta: string; titulo: string; detalle: string }> = {
  VALIDO: {
    etiqueta: 'Válido',
    titulo: 'Listo para enviar',
    detalle: 'No se encontraron errores ni advertencias.',
  },
  VALIDO_CON_ADVERTENCIAS: {
    etiqueta: 'Con advertencias',
    titulo: 'Se puede enviar, pero revisa las advertencias',
    detalle: 'No hay errores que bloqueen el envío; las advertencias suelen terminar en glosas.',
  },
  RECHAZADO: {
    etiqueta: 'Rechazado',
    titulo: 'Corrige los errores antes de enviarlo',
    detalle: 'El Ministerio rechazaría este archivo.',
  },
};
