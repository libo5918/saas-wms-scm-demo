INSERT INTO sales_order (
    id, tenant_id, order_no, warehouse_id, order_status, failure_reason, created_by, updated_by, deleted
) VALUES
    (1, 1, 'SO-001', 2001, 'LOCK_SUCCESS', NULL, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    warehouse_id = VALUES(warehouse_id),
    order_status = VALUES(order_status),
    failure_reason = VALUES(failure_reason),
    updated_by = VALUES(updated_by),
    deleted = VALUES(deleted);

INSERT INTO sales_order_item (
    id, tenant_id, sales_order_id, material_id, location_id, sale_qty
) VALUES
    (11, 1, 1, 1, 3001, 2.0000)
ON DUPLICATE KEY UPDATE
    sales_order_id = VALUES(sales_order_id),
    material_id = VALUES(material_id),
    location_id = VALUES(location_id),
    sale_qty = VALUES(sale_qty);
