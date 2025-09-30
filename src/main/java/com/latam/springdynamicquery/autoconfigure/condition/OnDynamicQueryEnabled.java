package com.latam.springdynamicquery.autoconfigure.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Condición personalizada para habilitar funcionalidad cuando dynamic query
 * está habilitado.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(value = "app.dynamic-query.enabled", havingValue = "true", matchIfMissing = true)
public @interface OnDynamicQueryEnabled {
}