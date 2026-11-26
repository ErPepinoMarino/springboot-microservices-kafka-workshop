CREATE TABLE stock_disponible (
    product_id VARCHAR(100) PRIMARY KEY,
    available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0)
);

CREATE TABLE registro_ordenes (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(100) NOT NULL,
    order_id BIGINT NULL,
    tipo_de_movimiento VARCHAR(10) NOT NULL
        CHECK (tipo_de_movimiento IN ('entrada', 'salida')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_event_id VARCHAR(200) NOT NULL UNIQUE,
    CONSTRAINT fk_registro_producto
        FOREIGN KEY (product_id) REFERENCES stock_disponible(product_id)
);
