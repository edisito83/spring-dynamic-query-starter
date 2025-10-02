package com.latam.springdynamicquery.util;

import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utilidades para manejo de recursos.
 */
public final class ResourceUtils {
    
    private ResourceUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Lee el contenido completo de un recurso como String.
     */
    public static String readResourceAsString(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    
    public static String getYamlBaseName(Resource resource) {
        String filename = resource.getFilename();
        
        if (!StringUtils.hasText(filename)) {
            return "unknown";
        }
        
        if (filename.endsWith(".yml") || filename.endsWith(".yaml")) {
            int dotIndex = filename.lastIndexOf('.');
            return filename.substring(0, dotIndex);
        }
        
        return filename;
    }
    
    /**
     * Verifica si un recurso es un archivo YAML.
     */
    public static boolean isYamlFile(Resource resource) {
        String filename = resource.getFilename();
        return StringUtils.hasText(filename) && 
               (filename.endsWith(".yml") || filename.endsWith(".yaml"));
    }
    
    /**
     * Obtiene la extensión de un recurso.
     */
    public static String getExtension(Resource resource) {
        String filename = resource.getFilename();
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex + 1) : "";
    }
}