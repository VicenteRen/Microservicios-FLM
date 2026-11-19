
-- Un pedido representa una compra completa del cliente.
CREATE TABLE pedidos (
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

-- Cada detalle representa un producto dentro del pedido.
CREATE TABLE detalle_pedido (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pedido_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    nombre_producto VARCHAR(255) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(15,2) NOT NULL,
    subtotal DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_detalle_pedido_pedido
        FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    CONSTRAINT uk_detalle_pedido_producto
        UNIQUE (pedido_id, producto_id)
);
