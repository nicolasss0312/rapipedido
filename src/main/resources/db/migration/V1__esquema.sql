-- ===== Catálogo =====
create table restaurantes (
    id          bigserial primary key,
    nombre      varchar(120) not null,
    direccion   varchar(200) not null,
    lat         double precision not null,
    lng         double precision not null,
    abierto     boolean not null default true
);

create table productos (
    id              bigserial primary key,
    restaurante_id  bigint not null references restaurantes (id),
    nombre          varchar(120) not null,
    descripcion     varchar(300),
    precio          numeric(12, 2) not null,
    disponible      boolean not null default true
);
create index idx_productos_restaurante on productos (restaurante_id);

-- ===== Pedidos =====
create table pedidos (
    id                  bigserial primary key,
    cliente_id          bigint not null,
    restaurante_id      bigint not null,
    estado              varchar(30) not null,
    metodo_pago         varchar(30) not null,
    direccion_entrega   varchar(200) not null,
    subtotal            numeric(12, 2) not null,
    costo_envio         numeric(12, 2) not null,
    comision            numeric(12, 2) not null,
    total               numeric(12, 2) not null,
    repartidor_id       bigint,
    clave_idempotencia  varchar(100) unique,
    creado_en           timestamptz not null,
    actualizado_en      timestamptz not null,
    version             bigint not null default 0
);
create index idx_pedidos_cliente on pedidos (cliente_id, creado_en desc);
create index idx_pedidos_restaurante on pedidos (restaurante_id, creado_en desc);
create index idx_pedidos_estado on pedidos (estado);

create table items_pedido (
    id               bigserial primary key,
    pedido_id        bigint not null references pedidos (id),
    producto_id      bigint not null,
    nombre           varchar(120) not null,
    precio_unitario  numeric(12, 2) not null,
    cantidad         integer not null,
    subtotal         numeric(12, 2) not null
);
create index idx_items_pedido on items_pedido (pedido_id);

-- ===== Pagos =====
create table pagos (
    id                  bigserial primary key,
    pedido_id           bigint not null unique,
    metodo              varchar(30) not null,
    monto               numeric(12, 2) not null,
    estado              varchar(20) not null,
    intentos            integer not null default 0,
    referencia_externa  varchar(100),
    motivo              varchar(300),
    creado_en           timestamptz not null,
    actualizado_en      timestamptz not null,
    version             bigint not null default 0
);
create index idx_pagos_pendientes on pagos (estado, actualizado_en);

-- ===== Asignación =====
create table repartidores (
    id        bigserial primary key,
    nombre    varchar(120) not null,
    telefono  varchar(30) not null,
    estado    varchar(20) not null,
    version   bigint not null default 0
);

create table asignaciones (
    id              bigserial primary key,
    pedido_id       bigint not null unique,
    restaurante_id  bigint not null,
    lat             double precision not null,
    lng             double precision not null,
    repartidor_id   bigint,
    estado          varchar(20) not null,
    creada_en       timestamptz not null,
    actualizada_en  timestamptz not null,
    version         bigint not null default 0
);
create index idx_asignaciones_estado on asignaciones (estado);

-- ===== Outbox de eventos =====
-- Los eventos se guardan en la MISMA transacción que el cambio de negocio y un relay los
-- envía después a RabbitMQ. Si RabbitMQ está caído no se pierde ningún evento.
create table outbox_eventos (
    id            bigserial primary key,
    tipo          varchar(100) not null,
    payload       text not null,
    creado_en     timestamptz not null,
    publicado_en  timestamptz
);
create index idx_outbox_pendientes on outbox_eventos (id) where publicado_en is null;
