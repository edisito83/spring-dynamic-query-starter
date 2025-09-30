package com.latam.springdynamicquery.exception;

/**
 * Excepción base para errores relacionados con consultas dinámicas.
 */
public class DynamicQueryException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

	public DynamicQueryException(String message) {
        super(message);
    }
    
    public DynamicQueryException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DynamicQueryException(Throwable cause) {
        super(cause);
    }
}