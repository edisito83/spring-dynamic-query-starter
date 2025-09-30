package com.latam.springdynamicquery.autoconfigure.condition;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Condición personalizada para verificar que Jackson YAML esté disponible.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnClass(name = "com.fasterxml.jackson.dataformat.yaml.YAMLFactory")
public @interface OnYamlSupported {
}