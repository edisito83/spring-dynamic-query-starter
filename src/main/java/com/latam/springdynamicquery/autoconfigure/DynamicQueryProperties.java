package com.latam.springdynamicquery.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propiedades de configuración para Spring Dynamic Query Starter.
 */
@Data
@ConfigurationProperties(prefix = "app.dynamic-query")
public class DynamicQueryProperties {
    
    /**
     * Habilita o deshabilita la funcionalidad de consultas dinámicas.
     */
    private boolean enabled = true;
    
    /**
     * Habilita la precarga de consultas SQL al inicio de la aplicación.
     */
    private boolean preloadEnabled = true;
    
    /**
     * Ruta base para los archivos de consulta SQL.
     */
    private String basePath = "sql";
    
    /**
     * Lista de patrones de paquetes para escanear archivos YAML de consultas.
     */
    private List<String> scanPackages = List.of("sql/*.yml", "sql/*.yaml");
    
    /**
     * Extensión de archivo para archivos YAML de consultas.
     */
    private String yamlExtension = "yml";
    
    /**
     * Codificación de caracteres para leer archivos de consultas.
     */
    private String encoding = "UTF-8";
    
    /**
     * Configuración de caché.
     */
    private Cache cache = new Cache();
    
    /**
     * Configuración de logging.
     */
    private Logging logging = new Logging();
    
    /**
     * Configuración de validación.
     */
    private Validation validation = new Validation();
    
    /**
     * Paquetes base para escanear repositories dinámicos.
     */
    private Repository repository = new Repository();
    
    @Data
    public static class Cache {
        /**
         * Habilita el caché de consultas parseadas.
         */
        private boolean enabled = true;
        
        /**
         * Número máximo de consultas a mantener en caché.
         */
        private int size = 1000;
        
        /**
         * Tiempo de vida del caché en minutos.
         */
        private long ttlMinutes = 60;
    }
    
    @Data
    public static class Logging {
        /**
         * Habilita logging detallado de ejecución de consultas.
         */
        private boolean enabled = false;
        
        /**
         * Habilita logging de parámetros de consulta (cuidado con datos sensibles).
         */
        private boolean logParameters = false;
        
        /**
         * Habilita logging de tiempo de ejecución de consultas.
         */
        private boolean logExecutionTime = true;
        
        /**
         * Habilita logging de SQL generado.
         */
        private boolean logGeneratedSql = false;
    }
    
    @Data
    public static class Validation {
        /**
         * Habilita validación estricta de archivos de consulta y parámetros.
         */
        private boolean strictMode = false;
        
        /**
         * Valida todas las consultas al inicio de la aplicación.
         */
        private boolean validateAtStartup = true;
        
        /**
         * Valida que todos los parámetros requeridos estén presentes.
         */
        private boolean validateRequiredParameters = true;
        
        /**
         * Valida la sintaxis SQL básica de las consultas.
         */
        private boolean validateSqlSyntax = false;
    }
    
    @Data
    public static class Repository {
        /**
         * Paquetes base donde buscar interfaces que extiendan DynamicRepository.
         */
        private String[] basePackages = {};
        
        /**
         * Habilita el factory personalizado para repositories dinámicos.
         */
        private boolean customFactoryEnabled = true;
    }
}