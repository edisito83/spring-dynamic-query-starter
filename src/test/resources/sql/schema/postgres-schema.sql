DROP TABLE IF EXISTS numero_awb;
DROP TABLE IF EXISTS guias_despacho;
DROP TABLE IF EXISTS clientes;
DROP TABLE IF EXISTS user_orders;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS departments;

CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL,
    created_date TIMESTAMP
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    age INT,
    status VARCHAR(50),
    active BOOLEAN NOT NULL,
    department_id BIGINT REFERENCES departments(id),
    salary NUMERIC(15,2),
    employee_number VARCHAR(50),
    created_date TIMESTAMP DEFAULT NOW(),
    updated_date TIMESTAMP DEFAULT NOW()
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    order_number VARCHAR(100) NOT NULL,
    total NUMERIC(15,2),
    status VARCHAR(50),
    created_date TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_orders (
    user_id BIGINT NOT NULL REFERENCES users(id),
    order_id BIGINT NOT NULL REFERENCES orders(id),
    role VARCHAR(50),
    PRIMARY KEY (user_id, order_id)
);

CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    rut VARCHAR(20),
    email VARCHAR(255),
    telefono VARCHAR(50),
    activo BOOLEAN NOT NULL,
    created_date TIMESTAMP
);

CREATE TABLE guias_despacho (
    id BIGSERIAL PRIMARY KEY,
    numero_guia VARCHAR(100) NOT NULL,
    estado VARCHAR(50),
    cliente_id BIGINT REFERENCES clientes(id),
    fecha_creacion TIMESTAMP DEFAULT NOW(),
    fecha_despacho TIMESTAMP,
    observaciones VARCHAR(255),
    active BOOLEAN
);

CREATE TABLE numero_awb (
    id BIGSERIAL PRIMARY KEY,
    guia_id BIGINT NOT NULL REFERENCES guias_despacho(id),
    numero VARCHAR(100) NOT NULL,
    tipo VARCHAR(50),
    activo BOOLEAN NOT NULL,
    created_date TIMESTAMP DEFAULT NOW()
);
