-- ==================== src/test/resources/sql/data/test-data.sql ====================
-- Test data for all databases

-- Insert departments
INSERT INTO departments (name, code, active) VALUES 
('Information Technology', 'IT', TRUE),
('Human Resources', 'HR', TRUE),
('Finance', 'FIN', TRUE),
('Marketing', 'MKT', TRUE),
('Operations', 'OPS', FALSE);

-- Insert users
INSERT INTO users (name, email, age, status, active, department_id, salary, employee_number) VALUES 
('John Doe', 'john.doe@company.com', 30, 'ACTIVE', TRUE, 1, 75000.00, 'EMP001'),
('Jane Smith', 'jane.smith@company.com', 28, 'ACTIVE', TRUE, 1, 70000.00, 'EMP002'),
('Bob Johnson', 'bob.johnson@company.com', 35, 'ACTIVE', TRUE, 2, 65000.00, 'EMP003'),
('Alice Brown', 'alice.brown@company.com', 32, 'INACTIVE', FALSE, 3, 80000.00, 'EMP004'),
('Charlie Wilson', 'charlie.wilson@company.com', 29, 'ACTIVE', TRUE, 1, 72000.00, 'EMP005'),
('Diana Davis', 'diana.davis@company.com', 27, 'ACTIVE', TRUE, 4, 68000.00, 'EMP006'),
('Test User', 'test@example.com', 25, 'ACTIVE', TRUE, 1, 50000.00, 'TEST123');

-- Insert orders
INSERT INTO orders (user_id, order_number, total, status) VALUES 
(1, 'ORD-2024-001', 1500.50, 'COMPLETED'),
(1, 'ORD-2024-002', 750.25, 'PENDING'),
(2, 'ORD-2024-003', 2200.00, 'COMPLETED'),
(3, 'ORD-2024-004', 899.99, 'SHIPPED'),
(5, 'ORD-2024-005', 1200.75, 'COMPLETED'),
(7, 'ORD-2024-006', 500.00, 'PENDING');

-- Insert user_orders relationships
INSERT INTO user_orders (user_id, order_id, role) VALUES 
(1, 1, 'CUSTOMER'),
(1, 2, 'CUSTOMER'),
(2, 3, 'CUSTOMER'),
(3, 4, 'CUSTOMER'),
(5, 5, 'CUSTOMER'),
(7, 6, 'CUSTOMER');

-- Insert clientes (for your specific use case)
INSERT INTO clientes (nombre, rut, email, telefono, activo) VALUES 
('Empresa ABC S.A.', '12345678-9', 'contacto@abc.cl', '+56912345678', TRUE),
('Comercial XYZ Ltda.', '98765432-1', 'info@xyz.cl', '+56987654321', TRUE),
('Test Client', '11111111-1', 'test@client.com', '+56911111111', TRUE);

-- Insert guias_despacho (for your specific use case)
INSERT INTO guias_despacho (numero_guia, estado, cliente_id, observaciones) VALUES 
('GD-2024-001', 'EN_TRANSITO', 1, 'Entrega normal'),
('GD-2024-002', 'ENTREGADA', 2, 'Entregado con éxito'),
('TEST123', 'CREADA', 3, 'Guía de prueba'),
('GD-2024-004', 'EN_TRANSITO', 1, 'Urgente'),
('ABC-12345', 'PENDIENTE', 2, 'Pendiente de confirmación');

-- Insert numeroawb (for your specific OR EXISTS use case)
INSERT INTO numero_awb (guia_id, numero, tipo, activo) VALUES 
(1, 'AWB001234567', 'AWB', TRUE),
(1, 'AWB001234568', 'TRACKING', TRUE),
(3, '12345', 'AWB', TRUE),  -- This will match the TEST123 search
(3, 'TEST-AWB-001', 'AWB', TRUE),
(4, 'URG789456123', 'EXPRESS', TRUE),
(5, '12345-ALT', 'AWB', TRUE);  -- This will also match 12345 searches

UPDATE guias_despacho SET fecha_creacion = '2024-01-02', fecha_despacho = '2024-12-30'
WHERE id = 3 AND numero_guia = 'TEST123';

-- Insert some historical data for date range testing
INSERT INTO guias_despacho (numero_guia, estado, cliente_id, fecha_creacion, observaciones) VALUES 
('OLD-2023-001', 'ENTREGADA', 1, '2023-12-01 10:00:00', 'Histórica'),
('OLD-2023-002', 'ENTREGADA', 2, '2023-11-15 14:30:00', 'Histórica');

-- ==================== src/test/resources/sql/data/integration-data.sql ====================
-- Additional integration test data

-- More complex scenarios for testing
INSERT INTO departments (name, code, active) VALUES 
('Research & Development', 'RD', TRUE),
('Customer Service', 'CS', TRUE),
('Quality Assurance', 'QA', FALSE);

INSERT INTO users (name, email, age, status, active, department_id, salary, employee_number) VALUES 
('Integration Test User 1', 'integration1@test.com', 30, 'ACTIVE', TRUE, 1, 60000.00, 'INT001'),
('Integration Test User 2', 'integration2@test.com', 35, 'ACTIVE', TRUE, 2, 65000.00, 'INT002'),
('Integration Test User 3', 'integration3@test.com', 25, 'INACTIVE', FALSE, 3, 55000.00, 'INT003');

-- Complex orders for integration testing
INSERT INTO orders (user_id, order_number, total, status) VALUES 
(8, 'INT-2024-001', 5000.00, 'COMPLETED'),
(8, 'INT-2024-002', 3500.00, 'SHIPPED'),
(9, 'INT-2024-003', 2800.00, 'COMPLETED'),
(10, 'INT-2024-004', 1200.00, 'CANCELLED');

-- Complex guias for your use case testing
INSERT INTO guias_despacho (numero_guia, estado, cliente_id, observaciones) VALUES 
('INTEGRATION-001', 'EN_TRANSITO', 1, 'Integration test guía'),
('COMPLEX-SEARCH-123', 'PENDIENTE', 2, 'Complex search test');

INSERT INTO numero_awb (guia_id, numero, tipo, activo) VALUES 
(8, 'INTEGRATION-AWB-001', 'AWB', TRUE),
(9, 'COMPLEX-123', 'AWB', TRUE),
(9, 'SEARCH-456', 'TRACKING', TRUE);