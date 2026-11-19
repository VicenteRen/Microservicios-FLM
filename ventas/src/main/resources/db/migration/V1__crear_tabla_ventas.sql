
-- Tabla principal de ventas
CREATE TABLE ventas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre_cliente VARCHAR(255) NOT NULL,
    apellidos_cliente VARCHAR(255) NOT NULL,
    correo_cliente VARCHAR(255) NOT NULL,
    direccion_envio VARCHAR(255) NOT NULL,
    region VARCHAR(255) NOT NULL,
    comuna VARCHAR(255) NOT NULL,
    total DECIMAL(15,2) NOT NULL,
    fecha_pedido DATETIME(6) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO',
    PRIMARY KEY (id)
);

-- Productos incluidos en cada venta
CREATE TABLE detalle_venta (
    id BIGINT NOT NULL AUTO_INCREMENT,
    venta_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    nombre_producto VARCHAR(255) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(15,2) NOT NULL,
    subtotal DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_detalle_venta
        FOREIGN KEY (venta_id) REFERENCES ventas(id),
    CONSTRAINT uk_detalle_venta_producto
        UNIQUE (venta_id, producto_id)
);
