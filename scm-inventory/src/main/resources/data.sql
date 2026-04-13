INSERT INTO inventory_balance (
    id, tenant_id, material_id, warehouse_id, location_id, on_hand_qty, locked_qty, available_qty, version, created_by, updated_by, deleted
) VALUES
    (1, 1, 1, 2001, 3001, 20.0000, 0.0000, 20.0000, 1, 1, 1, 0),
    (2, 1, 2, 2001, 3001, 10.0000, 0.0000, 10.0000, 1, 1, 1, 0),
    (3, 1, 3, 2001, 3002, 5.0000, 0.0000, 5.0000, 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    on_hand_qty = VALUES(on_hand_qty),
    locked_qty = VALUES(locked_qty),
    available_qty = VALUES(available_qty),
    version = VALUES(version),
    updated_by = VALUES(updated_by),
    deleted = VALUES(deleted);

INSERT INTO inventory_txn_record (
    id, tenant_id, txn_no, biz_type, biz_no, material_id, warehouse_id, location_id, txn_direction, txn_qty, before_qty, after_qty
) VALUES
    (1, 1, 'IN-20260413-001', 'PURCHASE_RECEIPT', 'RCV-001', 1, 2001, 3001, 'IN', 20.0000, 0.0000, 20.0000),
    (2, 1, 'IN-20260413-002', 'PURCHASE_RECEIPT', 'RCV-001', 2, 2001, 3001, 'IN', 10.0000, 0.0000, 10.0000),
    (3, 1, 'IN-20260413-003', 'PURCHASE_RECEIPT', 'RCV-002', 3, 2001, 3002, 'IN', 5.0000, 0.0000, 5.0000)
ON DUPLICATE KEY UPDATE
    biz_type = VALUES(biz_type),
    biz_no = VALUES(biz_no),
    material_id = VALUES(material_id),
    warehouse_id = VALUES(warehouse_id),
    location_id = VALUES(location_id),
    txn_direction = VALUES(txn_direction),
    txn_qty = VALUES(txn_qty),
    before_qty = VALUES(before_qty),
    after_qty = VALUES(after_qty);
