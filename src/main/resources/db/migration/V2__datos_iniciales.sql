insert into restaurantes (nombre, direccion, lat, lng, abierto) values
    ('La Brasa Dorada',  'Cra 15 # 93-20, Bogotá',  4.6766, -74.0485, true),
    ('Sushi Andino',     'Calle 70 # 5-12, Bogotá', 4.6560, -74.0560, true),
    ('Arepas del Centro','Cra 7 # 22-40, Bogotá',   4.6097, -74.0705, true);

insert into productos (restaurante_id, nombre, descripcion, precio, disponible) values
    (1, 'Pollo asado entero', 'Con papa salada y arepa',     42000, true),
    (1, 'Medio pollo',        'Con papa francesa',           24000, true),
    (1, 'Limonada de coco',   '400 ml',                       9000, true),
    (2, 'Roll acevichado',    '10 piezas',                   36000, true),
    (2, 'Gyozas',             '6 unidades de cerdo',         18000, true),
    (2, 'Té verde frío',      '350 ml',                       7000, true),
    (3, 'Arepa de choclo',    'Con queso campesino',         12000, true),
    (3, 'Arepa rellena',      'Carne desmechada y aguacate', 16000, true),
    (3, 'Jugo de lulo',       'En agua o en leche',           6500, true);

insert into repartidores (nombre, telefono, estado) values
    ('Andrés Rojas',   '3001112233', 'DISPONIBLE'),
    ('Camila Torres',  '3002223344', 'DISPONIBLE'),
    ('Felipe Gómez',   '3003334455', 'DISPONIBLE'),
    ('Laura Martínez', '3004445566', 'DISPONIBLE'),
    ('Santiago Pérez', '3005556677', 'DISPONIBLE'),
    ('Valentina Ruiz', '3006667788', 'DISPONIBLE');
