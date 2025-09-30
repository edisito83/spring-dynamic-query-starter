package com.latam.springdynamicquery.exception;

/**
 * Excepción lanzada cuando una consulta tiene un formato o configuración inválida.
 */
public class InvalidQueryException extends DynamicQueryException {
    
    private static final long serialVersionUID = 1L;
	private final String queryName;
    private final String reason;
    
    public InvalidQueryException(String queryName, String reason) {
        super("Invalid query '" + queryName + "': " + reason);
        this.queryName = queryName;
        this.reason = reason;
    }
    
    public InvalidQueryException(String queryName, String reason, Throwable cause) {
        super("Invalid query '" + queryName + "': " + reason, cause);
        this.queryName = queryName;
        this.reason = reason;
    }
    
    public String getQueryName() {
        return queryName;
    }
    
    public String getReason() {
        return reason;
    }
}