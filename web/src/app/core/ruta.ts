/** Nombres legibles de los campos del RIPS para mostrar las rutas de los hallazgos. */
const CAMPOS: Record<string, string> = {
  numDocumentoIdObligado: 'NIT del obligado',
  numFactura: 'Número de factura',
  tipoNota: 'Tipo de nota',
  numNota: 'Número de nota',
  usuarios: 'Usuarios',
  tipoDocumentoIdentificacion: 'Tipo de documento',
  numDocumentoIdentificacion: 'Número de documento',
  tipoUsuario: 'Tipo de usuario',
  fechaNacimiento: 'Fecha de nacimiento',
  codSexo: 'Sexo',
  codPaisResidencia: 'País de residencia',
  codMunicipioResidencia: 'Municipio de residencia',
  codZonaTerritorialResidencia: 'Zona territorial',
  incapacidad: 'Incapacidad',
  consecutivo: 'Consecutivo',
  codPaisOrigen: 'País de origen',
  servicios: 'Servicios',
  codPrestador: 'Código del prestador',
  fechaInicioAtencion: 'Fecha de atención',
  codConsulta: 'Código CUPS',
  codProcedimiento: 'Código CUPS',
  codDiagnosticoPrincipal: 'Diagnóstico principal',
  codDiagnosticoRelacionado: 'Diagnóstico relacionado',
  codDiagnosticoRelacionado1: 'Diagnóstico relacionado 1',
  codDiagnosticoRelacionado2: 'Diagnóstico relacionado 2',
  codDiagnosticoRelacionado3: 'Diagnóstico relacionado 3',
  codComplicacion: 'Complicación',
  tipoDiagnosticoPrincipal: 'Tipo de diagnóstico',
  modalidadGrupoServicioTecSal: 'Modalidad de atención',
  grupoServicios: 'Grupo de servicios',
  finalidadTecnologiaSalud: 'Finalidad',
  causaMotivoAtencion: 'Causa de la atención',
  viaIngresoServicioSalud: 'Vía de ingreso',
  codDiagnosticoPrincipalE: 'Diagnóstico principal de egreso',
  codDiagnosticoRelacionadoE1: 'Diagnóstico relacionado de egreso 1',
  codDiagnosticoRelacionadoE2: 'Diagnóstico relacionado de egreso 2',
  codDiagnosticoRelacionadoE3: 'Diagnóstico relacionado de egreso 3',
  codDiagnosticoCausaMuerte: 'Causa de muerte',
  condicionDestinoUsuarioEgreso: 'Condición al egreso',
  fechaEgreso: 'Fecha de egreso',
  fechaDispensAdmon: 'Fecha de dispensación',
  fechaSuministroTecnologia: 'Fecha de suministro',
  tipoMedicamento: 'Tipo de medicamento',
  codTecnologiaSalud: 'Código de la tecnología',
  unidadMedida: 'Unidad de medida',
  formaFarmaceutica: 'Forma farmacéutica',
  cantidadMedicamento: 'Cantidad',
  tipoOS: 'Tipo de servicio',
  cantidadOS: 'Cantidad',
  peso: 'Peso',
  vrServicio: 'Valor del servicio',
  valorPagoModerador: 'Pago moderador',
  conceptoRecaudo: 'Concepto de recaudo',
};

const ELEMENTOS: Record<string, string> = {
  usuarios: 'Usuario',
  consultas: 'Consulta',
  procedimientos: 'Procedimiento',
  urgencias: 'Urgencia',
  hospitalizacion: 'Hospitalización',
  recienNacidos: 'Recién nacido',
  medicamentos: 'Medicamento',
  otrosServicios: 'Otro servicio',
};

/**
 * Convierte una ruta de la API en migas de pan legibles:
 * {@code usuarios[0].servicios.consultas[1].codConsulta} → ["Usuario 1", "Consulta 2", "Código CUPS"].
 * Los índices se muestran desde 1, como los cuenta una persona.
 */
export function describirRuta(ruta: string): string[] {
  const partes = ruta.split('.');
  return partes
    .filter((parte, i) => parte !== 'servicios' || i === partes.length - 1)
    .map((parte) => {
      const elemento = /^(\w+)\[(\d+)\]$/.exec(parte);
      if (elemento) {
        const [, coleccion, indice] = elemento;
        return `${ELEMENTOS[coleccion] ?? coleccion} ${Number(indice) + 1}`;
      }
      return CAMPOS[parte] ?? parte;
    });
}
