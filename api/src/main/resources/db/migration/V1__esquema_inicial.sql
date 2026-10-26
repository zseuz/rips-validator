-- Esquema inicial. SQL común a H2 (desarrollo y pruebas) y MySQL 8 (docker compose).
-- No se edita una migración ya aplicada: los cambios van en un archivo V2__..., V3__...

-- Una fila por archivo RIPS validado. El RIPS no se guarda: solo el resumen.
create table validacion (
    id                        bigint       not null auto_increment,
    num_factura               varchar(20),
    num_documento_id_obligado varchar(20),
    estado                    varchar(25)  not null,
    total_usuarios            integer      not null,
    total_servicios           integer      not null,
    total_errores             bigint       not null,
    total_advertencias        bigint       not null,
    fecha_validacion          timestamp(6) not null,
    primary key (id)
);

create index ix_validacion_fecha on validacion (fecha_validacion);

-- Hallazgos de cada validación, en el orden en que se encontraron.
create table hallazgo (
    validacion_id bigint       not null,
    orden         integer      not null,
    codigo        varchar(5)   not null,
    severidad     varchar(12)  not null,
    ruta          varchar(255) not null,
    mensaje       varchar(500) not null,
    valor         varchar(255),
    primary key (validacion_id, orden),
    constraint fk_hallazgo_validacion foreign key (validacion_id) references validacion (id)
);

create index ix_hallazgo_codigo on hallazgo (codigo);

-- Copia de las tablas de referencia de SISPRO; la API la recarga completa al arrancar.
create table item_referencia (
    tabla       varchar(40)  not null,
    codigo      varchar(20)  not null,
    nombre      varchar(500),
    habilitado  boolean      not null,
    sexo        varchar(1),
    edad_minima integer,
    edad_maxima integer,
    uso         varchar(2),
    primary key (tabla, codigo)
);
