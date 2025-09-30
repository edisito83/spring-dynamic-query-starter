package com.latam.springdynamicquery.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Utilidades de reflexión para el framework.
 */
public final class ReflectionUtils {
    
    private ReflectionUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Resuelve el tipo genérico de una clase en la posición especificada.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> resolveGenericType(Class<?> clazz, int index) {
        Type genericSuperclass = clazz.getGenericSuperclass();
        
        if (genericSuperclass instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length > index) {
                Type type = actualTypeArguments[index];
                if (type instanceof Class) {
                    return (Class<T>) type;
                }
            }
        }
        
        throw new IllegalArgumentException("Could not resolve generic type at index " + index + " for class " + clazz.getName());
    }
    
    /**
     * Verifica si una clase tiene un tipo genérico específico.
     */
    public static boolean hasGenericType(Class<?> clazz) {
        Type genericSuperclass = clazz.getGenericSuperclass();
        return genericSuperclass instanceof ParameterizedType;
    }
    
    /**
     * Obtiene todos los tipos genéricos de una clase.
     */
    public static Type[] getAllGenericTypes(Class<?> clazz) {
        Type genericSuperclass = clazz.getGenericSuperclass();
        
        if (genericSuperclass instanceof ParameterizedType parameterizedType) {
            return parameterizedType.getActualTypeArguments();
        }
        
        return new Type[0];
    }
}