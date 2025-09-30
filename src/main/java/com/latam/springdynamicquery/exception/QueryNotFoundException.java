package com.latam.springdynamicquery.exception;

/**
 * Excepción lanzada cuando no se encuentra una consulta solicitada.
 */
public class QueryNotFoundException extends DynamicQueryException {
    
    private static final long serialVersionUID = 1L;
	private final String queryName;
    
    public QueryNotFoundException(String queryName) {
        super("Query not found: " + queryName);
        this.queryName = queryName;
    }
    
    public QueryNotFoundException(String queryName, String availableQueries) {
        super("Query not found: " + queryName + ". Available queries: " + availableQueries);
        this.queryName = queryName;
    }
    
    public String getQueryName() {
        return queryName;
    }
}