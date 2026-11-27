ALTER TABLE registro_ordenes
    DROP COLUMN source_event_id;

CREATE TABLE processed_order (
    order_id BIGINT PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);