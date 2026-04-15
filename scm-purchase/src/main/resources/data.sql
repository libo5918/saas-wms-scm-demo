INSERT INTO purchase_order (
    id, tenant_id, order_no, supplier_id, order_status, total_amount, remark, created_by, updated_by, deleted
) VALUES
    (1, 1, 'PO-001', 1, 'CREATED', 300.0000, '首单采购', 1, 1, 0),
    (2, 1, 'PO-002', 2, 'CREATED', 150.0000, '备件采购', 1, 1, 0)
ON DUPLICATE KEY UPDATE
    supplier_id = VALUES(supplier_id),
    order_status = VALUES(order_status),
    total_amount = VALUES(total_amount),
    remark = VALUES(remark),
    updated_by = VALUES(updated_by),
    deleted = VALUES(deleted);

INSERT INTO purchase_order_item (
    id, tenant_id, purchase_order_id, material_id, plan_qty, received_qty, unit_price
) VALUES
    (11, 1, 1, 1, 20.0000, 0.0000, 10.0000),
    (12, 1, 1, 2, 10.0000, 0.0000, 10.0000),
    (13, 1, 2, 3, 5.0000, 0.0000, 30.0000)
ON DUPLICATE KEY UPDATE
    purchase_order_id = VALUES(purchase_order_id),
    material_id = VALUES(material_id),
    plan_qty = VALUES(plan_qty),
    received_qty = VALUES(received_qty),
    unit_price = VALUES(unit_price);

INSERT INTO purchase_receipt (
    id, tenant_id, receipt_no, purchase_order_id, supplier_id, warehouse_id, receipt_status, failure_reason, created_by, updated_by, deleted
) VALUES
    (1, 1, 'RCV-001', 1, 1, 2001, 'STOCK_IN_SUCCESS', NULL, 1, 1, 0),
    (2, 1, 'RCV-002', 2, 2, 2001, 'STOCK_IN_SUCCESS', NULL, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    purchase_order_id = VALUES(purchase_order_id),
    supplier_id = VALUES(supplier_id),
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
