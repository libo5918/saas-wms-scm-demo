INSERT INTO purchase_receipt (
    id, tenant_id, receipt_no, purchase_order_id, warehouse_id, receipt_status, failure_reason, created_by, updated_by, deleted
) VALUES
    (1, 1, 'RCV-001', 5001, 2001, 'STOCK_IN_SUCCESS', NULL, 1, 1, 0),
    (2, 1, 'RCV-002', 5002, 2001, 'STOCK_IN_SUCCESS', NULL, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    purchase_order_id = VALUES(purchase_order_id),
    warehouse_id = VALUES(warehouse_id),
    receipt_status = VALUES(receipt_status),
    failure_reason = VALUES(failure_reason),
    updated_by = VALUES(updated_by),
    deleted = VALUES(deleted);

INSERT INTO purchase_receipt_item (
    id, tenant_id, purchase_receipt_id, material_id, location_id, receipt_qty
) VALUES
    (11, 1, 1, 1, 3001, 20.0000),
    (12, 1, 1, 2, 3001, 10.0000),
    (13, 1, 2, 3, 3002, 5.0000)
ON DUPLICATE KEY UPDATE
    purchase_receipt_id = VALUES(purchase_receipt_id),
    material_id = VALUES(material_id),
    location_id = VALUES(location_id),
    receipt_qty = VALUES(receipt_qty);
