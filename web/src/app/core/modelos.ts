/** Tipos de la API (ver api/src/main/java/com/zseuz/rips/validacion). */

export type Severidad = 'ERROR' | 'ADVERTENCIA';

export type EstadoValidacion = 'VALIDO' | 'VALIDO_CON_ADVERTENCIAS' | 'RECHAZADO';

export interface Hallazgo {
  codigo: string;
  severidad: Severidad;
  ruta: string;
  mensaje: string;
  valor: string | null;
}

export interface ResumenValidacion {
  id: number;
  numFactura: string | null;
  numDocumentoIdObligado: string | null;
  estado: EstadoValidacion;
  fechaValidacion: string;
  totalUsuarios: number;
  totalServicios: number;
  totalErrores: number;
  totalAdvertencias: number;
}

export interface ResultadoValidacion extends ResumenValidacion {
  hallazgos: Hallazgo[];
}

export interface Pagina<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface Regla {
  codigo: string;
  severidad: Severidad;
  titulo: string;
}
