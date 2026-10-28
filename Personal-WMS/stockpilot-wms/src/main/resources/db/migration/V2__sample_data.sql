-- Sample data so the system is usable on first start.
-- Users are created by DataInitializer (Java) because passwords must be hashed.

INSERT INTO uom (code, name, factor_to_each) VALUES
 ('EA',  'Each',               1),
 ('BX',  'Box of 10',         10),
 ('CS',  'Case of 12',        12),
 ('PLT', 'Pallet of 48 cases', 576);

INSERT INTO location (code, name, zone, type, active) VALUES
 ('RCV-01',  'Receiving dock',          'DOCK', 'RECEIVING', TRUE),
 ('A-01-01', 'Aisle A, bay 1, level 1', 'A',    'STORAGE',   TRUE),
 ('A-01-02', 'Aisle A, bay 1, level 2', 'A',    'STORAGE',   TRUE),
 ('A-02-01', 'Aisle A, bay 2, level 1', 'A',    'STORAGE',   TRUE),
 ('B-01-01', 'Aisle B, bay 1, level 1', 'B',    'STORAGE',   TRUE),
 ('B-01-02', 'Aisle B, bay 1, level 2', 'B',    'STORAGE',   TRUE),
 ('SHP-01',  'Shipping staging',        'DOCK', 'SHIPPING',  TRUE);

INSERT INTO part (part_number, description, standard_cost, reorder_point, qty_allocated, active, created_at) VALUES
 ('BLT-1420', 'Bolt, hex 1/4-20 x 1in, zinc',   0.1200, 200, 0,  TRUE, NOW(6)),
 ('NUT-1420', 'Nut, hex 1/4-20, zinc',          0.0500, 200, 0,  TRUE, NOW(6)),
 ('ZIP-08',   'Zip tie 8in black',              0.1000, 500, 0,  TRUE, NOW(6)),
 ('CLP-050',  'Cable clip 1/2in',               1.1500,  50, 0,  TRUE, NOW(6)),
 ('GLV-L',    'Work gloves, large',             4.7500,  24, 12, TRUE, NOW(6)),
 ('TAPE-48',  'Packing tape 48mm x 100m',       2.3000,  36, 0,  TRUE, NOW(6)),
 ('BOX-1212', 'Shipping box 12x12x12',          0.8900, 100, 0,  TRUE, NOW(6)),
 ('LBL-4X6',  'Thermal label 4x6, roll of 250', 9.5000,  10, 0,  TRUE, NOW(6));

INSERT INTO vendor (code, name, email, phone) VALUES
 ('FAST01', 'Fastener Supply Co.', 'orders@fastsupply.example', '555-0101'),
 ('PACK01', 'Packaging Direct',    'sales@packdirect.example',  '555-0102'),
 ('SAFE01', 'SafeHands Workwear',  'po@safehands.example',      '555-0103');

INSERT INTO customer (code, name, email, ship_to) VALUES
 ('ACME',  'Acme Builders',      'purchasing@acme.example',     '100 Main St, Atlanta, GA 30303'),
 ('PEACH', 'Peachtree Electric', 'ops@peachtree.example',       '2200 Peachtree Rd, Atlanta, GA 30309'),
 ('WEB',   'Web store customer', 'webstore@stockpilot.example', 'Varies per order');

-- Starting stock (in EA)
INSERT INTO inventory_level (part_id, location_id, qty_on_hand)
SELECT p.id, l.id, x.qty FROM (
  SELECT 'BLT-1420' AS pn, 'A-01-01' AS loc, 600 AS qty UNION ALL
  SELECT 'NUT-1420', 'A-01-02', 800  UNION ALL
  SELECT 'ZIP-08',   'A-02-01', 1500 UNION ALL
  SELECT 'CLP-050',  'A-02-01', 40   UNION ALL
  SELECT 'GLV-L',    'B-01-01', 10   UNION ALL
  SELECT 'GLV-L',    'B-01-02', 40   UNION ALL
  SELECT 'TAPE-48',  'B-01-01', 72   UNION ALL
  SELECT 'BOX-1212', 'B-01-02', 250  UNION ALL
  SELECT 'LBL-4X6',  'B-01-01', 6
) x
JOIN part p ON p.part_number = x.pn
JOIN location l ON l.code = x.loc;

INSERT INTO inventory_transaction (part_id, location_id, qty_change, type, reference, note, username, created_at)
SELECT il.part_id, il.location_id, il.qty_on_hand, 'ADJUST', 'OPENING', 'Opening balance', 'system', NOW(6)
FROM inventory_level il;

-- An issued purchase order waiting to be received
INSERT INTO purchase_order (po_number, vendor_id, status, order_date, expected_date, notes, created_at)
SELECT 'PO-00001', v.id, 'ISSUED', CURRENT_DATE, CURRENT_DATE, 'Restock cable management', NOW(6)
FROM vendor v WHERE v.code = 'FAST01';

INSERT INTO purchase_order_line (purchase_order_id, line_no, part_id, uom_id, qty_ordered, qty_received, unit_cost, status)
SELECT po.id, 1, p.id, u.id, 10, 0, 0.1000, 'OPEN'
FROM purchase_order po, part p, uom u
WHERE po.po_number = 'PO-00001' AND p.part_number = 'ZIP-08' AND u.code = 'EA';

INSERT INTO purchase_order_line (purchase_order_id, line_no, part_id, uom_id, qty_ordered, qty_received, unit_cost, status)
SELECT po.id, 2, p.id, u.id, 3, 0, 1.1500, 'OPEN'
FROM purchase_order po, part p, uom u
WHERE po.po_number = 'PO-00001' AND p.part_number = 'CLP-050' AND u.code = 'EA';

-- A PO ordered in cases, good for practicing partial receipts
INSERT INTO purchase_order (po_number, vendor_id, status, order_date, expected_date, notes, created_at)
SELECT 'PO-00002', v.id, 'ISSUED', CURRENT_DATE, CURRENT_DATE, 'Packing supplies', NOW(6)
FROM vendor v WHERE v.code = 'PACK01';

INSERT INTO purchase_order_line (purchase_order_id, line_no, part_id, uom_id, qty_ordered, qty_received, unit_cost, status)
SELECT po.id, 1, p.id, u.id, 10, 0, 27.6000, 'OPEN'
FROM purchase_order po, part p, uom u
WHERE po.po_number = 'PO-00002' AND p.part_number = 'TAPE-48' AND u.code = 'CS';

-- An issued sales order with stock already allocated (GLV-L qty_allocated = 12 above)
INSERT INTO sales_order (so_number, customer_id, status, order_date, external_ref, source, created_at)
SELECT 'SO-00001', c.id, 'ISSUED', CURRENT_DATE, NULL, 'MANUAL', NOW(6)
FROM customer c WHERE c.code = 'ACME';

INSERT INTO sales_order_line (sales_order_id, line_no, part_id, qty_ordered, qty_picked, unit_price)
SELECT so.id, 1, p.id, 12, 0, 8.9900
FROM sales_order so, part p
WHERE so.so_number = 'SO-00001' AND p.part_number = 'GLV-L';
