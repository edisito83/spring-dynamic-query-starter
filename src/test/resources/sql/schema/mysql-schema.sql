DROP TABLE IF EXISTS numero_awb;
DROP TABLE IF EXISTS guias_despacho;
DROP TABLE IF EXISTS clientes;
DROP TABLE IF EXISTS user_orders;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS departments;

CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    active TINYINT(1) NOT NULL,
    created_date DATETIME
);

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    age INT,
    status VARCHAR(50),
    active TINYINT(1) NOT NULL,
    department_id BIGINT,
    salary DECIMAL(15,2),
    employee_number VARCHAR(50),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES departments(id)
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_number VARCHAR(100) NOT NULL,
    total DECIMAL(15,2),
    status VARCHAR(50),
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_orders (
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    role VARCHAR(50),
    PRIMARY KEY (user_id, order_id),
    CONSTRAINT fk_uo_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_uo_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    rut VARCHAR(20),
    email VARCHAR(255),
    telefono VARCHAR(50),
    activo TINYINT(1) NOT NULL,
    created_date DATETIME
);

CREATE TABLE guias_despacho (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_guia VARCHAR(100) NOT NULL,
    estado VARCHAR(50),
    cliente_id BIGINT,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_despacho DATETIME,
    observaciones VARCHAR(255),
    active TINYINT(1),
    CONSTRAINT fk_guia_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

CREATE TABLE numero_awb (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guia_id BIGINT NOT NULL,
    numero VARCHAR(100) NOT NULL,
    tipo VARCHAR(50),
    activo TINYINT(1) NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_awb_guia FOREIGN KEY (guia_id) REFERENCES guias_despacho(id)
);
