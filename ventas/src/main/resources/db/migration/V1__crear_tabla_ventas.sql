
CREATE TABLE ventas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre_cliente VARCHAR(255) NOT NULL,
    apellidos_cliente VARCHAR(255),
    correo_cliente VARCHAR(255) NOT NULL,
    direccion_envio VARCHAR(255) NOT NULL,
    region VARCHAR(255),
    comuna VARCHAR(255),
    producto_id BIGINT NOT NULL,
    nombre_producto VARCHAR(255),
    cantidad INT NOT NULL,
    total DOUBLE NOT NULL,
    fecha_venta DATETIME(6),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    PRIMARY KEY (id)
);
