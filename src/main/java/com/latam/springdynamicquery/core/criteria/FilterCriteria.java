package com.latam.springdynamicquery.core.criteria;

import java.util.Collection;
import java.util.function.Supplier;

import com.latam.springdynamicquery.core.validation.ParameterValidator;
import com.latam.springdynamicquery.core.validation.QueryValidator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Representa un criterio de filtrado dinámico para consultas SQL. Permite
 * construir condiciones WHERE dinámicamente basadas en la presencia y validez
 * de valores.
 */
@Slf4j
@Getter
public class FilterCriteria {

	private final String sqlFragment;
	private final Object value;
	private final String connector;
	private final Supplier<Boolean> condition;
	private final boolean securityValidated;
	
	/**
     * Realiza validación de seguridad del fragmento SQL.
     */
    private void performSecurityValidation(String sqlFragment, Object value) {
        try {
        	QueryValidator.validateFilterCriteriaSafety(sqlFragment, value);
        } catch (Exception e) {
            log.error("Security validation failed for FilterCriteria: {}", e.getMessage());
            throw e;
        }
    }
	
	/**
     * Constructor principal para FilterCriteria.
     * 
     * @param sqlFragment El fragmento SQL a agregar si la condición se cumple
     * @param value       El valor del parámetro
     * @param connector   El conector SQL (AND, OR)
     * @param condition   La condición que determina si aplicar este criterio
     * @param validateSecurity Si true, valida el fragmento contra SQL injection
     */
    private FilterCriteria(String sqlFragment, Object value, String connector, 
                          Supplier<Boolean> condition, boolean validateSecurity) {
        this.sqlFragment = sqlFragment;
        this.value = value;
        this.connector = connector != null ? connector : "AND";
        this.condition = condition != null ? condition : () -> ParameterValidator.isValidValue(value);
        this.securityValidated = validateSecurity;
        
        // Validación de seguridad
        if (validateSecurity) {
            performSecurityValidation(sqlFragment, value);
        }
    }

	/**
	 * Constructor principal con validación de seguridad habilitada por defecto.
	 * 
	 * @param sqlFragment El fragmento SQL a agregar si la condición se cumple
	 * @param value       El valor del parámetro
	 * @param connector   El conector SQL (AND, OR)
	 * @param condition   La condición que determina si aplicar este criterio
	 */
	public FilterCriteria(String sqlFragment, Object value, String connector, Supplier<Boolean> condition) {
		this(sqlFragment, value, connector, condition, true);
	}

	/**
	 * Constructor simplificado con conector especificado.
	 */
	public FilterCriteria(String sqlFragment, Object value, String connector) {
		this(sqlFragment, value, connector, null, true);
	}

	/**
	 * Constructor más simple con conector AND por defecto.
	 */
	public FilterCriteria(String sqlFragment, Object value) {
		this(sqlFragment, value, "AND", null, true);
	}

	// ==================== Factory Methods ====================

	/**
	 * Crea un criterio que se aplica cuando el valor es válido.
	 */
	public static FilterCriteria when(String sqlFragment, Object value) {
		return new FilterCriteria(sqlFragment, value);
	}

	/**
	 * Crea un criterio que se aplica cuando el valor es válido, con conector
	 * especificado.
	 */
	public static FilterCriteria when(String sqlFragment, Object value, String connector) {
		return new FilterCriteria(sqlFragment, value, connector);
	}

	/**
	 * Crea un criterio que se aplica solo cuando el valor no es nulo.
	 */
	public static FilterCriteria whenNotNull(String sqlFragment, Object value) {
		return new FilterCriteria(sqlFragment, value, "AND", () -> value != null);
	}

	/**
     * Crea un criterio que se aplica cuando el valor no está vacío.
     * Funciona con strings, colecciones y arrays.
     * Detecta strings con "null" literal y espacios en blanco.
     */
    public static FilterCriteria whenNotEmpty(String sqlFragment, Object value) {
        return new FilterCriteria(sqlFragment, value, "AND", 
            () -> ParameterValidator.isValidValue(value));
    }
    
    /**
     * Crea un criterio que se aplica cuando el string es válido.
     * Rechaza null, vacío, espacios en blanco, y "null" literal.
     */
    public static FilterCriteria whenValidString(String sqlFragment, String value) {
        return new FilterCriteria(sqlFragment, value, "AND", 
            () -> ParameterValidator.isValidString(value));
    }
    
    /**
     * Crea un criterio que se aplica cuando la colección no está vacía.
     */
    public static FilterCriteria whenNotEmptyCollection(String sqlFragment, Collection<?> value) {
        return new FilterCriteria(sqlFragment, value, "AND", 
            () -> ParameterValidator.isValidCollection(value));
    }
    
    /**
     * Crea un criterio que se aplica cuando el valor es un número válido.
     * Acepta cero, positivos y negativos. Rechaza NaN e Infinity.
     */
    public static FilterCriteria whenNumericValid(String sqlFragment, Object value) {
        return new FilterCriteria(sqlFragment, value, "AND", 
            () -> ParameterValidator.isValidNumericValue(value));
    }

	/**
	 * Crea un criterio que se aplica cuando el valor es un número positivo.
	 */
	public static FilterCriteria whenNumericPositive(String sqlFragment, Object value) {
		return new FilterCriteria(sqlFragment, value, "AND", () -> ParameterValidator.isPositiveNumber(value));
	}

	/**
	 * Crea un criterio que se aplica cuando el valor booleano es true.
	 */
	public static FilterCriteria whenTrue(String sqlFragment, Boolean value) {
		return new FilterCriteria(sqlFragment, value, "AND", () -> Boolean.TRUE.equals(value));
	}

	/**
	 * Crea un criterio que se aplica cuando el valor booleano es false.
	 */
	public static FilterCriteria whenFalse(String sqlFragment, Boolean value) {
		return new FilterCriteria(sqlFragment, value, "AND", () -> Boolean.FALSE.equals(value));
	}
	
	/**
     * Crea un criterio que se aplica cuando el patrón LIKE es válido.
     * Valida que no sea null, vacío, "%%", o contenga "null" literal.
     */
    public static FilterCriteria whenLike(String sqlFragment, String pattern) {
        return new FilterCriteria(sqlFragment, pattern, "AND", 
            () -> ParameterValidator.isValidLikePattern(pattern));
    }
    
    /**
     * Crea un criterio que se aplica cuando el email es válido.
     */
    public static FilterCriteria whenValidEmail(String sqlFragment, String email) {
        return new FilterCriteria(sqlFragment, email, "AND", 
            () -> ParameterValidator.isValidEmail(email));
    }

	/**
	 * Crea un criterio que siempre se aplica.
	 */
	public static FilterCriteria always(String sqlFragment) {
		return new FilterCriteria(sqlFragment, null, "AND", () -> true);
	}

	/**
	 * Crea un criterio que nunca se aplica.
	 */
	public static FilterCriteria never(String sqlFragment) {
		return new FilterCriteria(sqlFragment, null, "AND", () -> false);
	}

	/**
	 * Crea un criterio con una condición personalizada.
	 */
	public static FilterCriteria withCondition(String sqlFragment, Object value, Supplier<Boolean> condition) {
		return new FilterCriteria(sqlFragment, value, "AND", condition);
	}

	/**
	 * Crea un criterio con una condición personalizada y conector especificado.
	 */
	public static FilterCriteria withCondition(String sqlFragment, Object value, String connector,
			Supplier<Boolean> condition) {
		return new FilterCriteria(sqlFragment, value, connector, condition);
	}
	
	/**
     * Crea un criterio sin validación de seguridad (USAR CON PRECAUCIÓN).
     * Solo para casos donde el fragmento SQL es totalmente controlado y confiable.
     * 
     * @deprecated Usar solo en casos excepcionales donde la validación de seguridad
     *             interfiere con lógica SQL compleja y válida.
     */
    @Deprecated
    public static FilterCriteria unsafe(String sqlFragment, Object value) {
        log.warn("Creating UNSAFE FilterCriteria without security validation. " +
                "Fragment: '{}'. Ensure this SQL is trusted and sanitized.", sqlFragment);
        return new FilterCriteria(sqlFragment, value, "AND", null, false);
    }

    /**
     * Crea un criterio sin validación de seguridad con conector especificado.
     * 
     * @deprecated Usar solo en casos excepcionales.
     */
    @Deprecated
    public static FilterCriteria unsafe(String sqlFragment, Object value, String connector) {
        log.warn("Creating UNSAFE FilterCriteria without security validation. " +
                "Fragment: '{}'. Ensure this SQL is trusted and sanitized.", sqlFragment);
        return new FilterCriteria(sqlFragment, value, connector, null, false);
    }

	// ==================== Métodos principales ====================

	/**
	 * Determina si este criterio debe aplicarse a la consulta.
	 * 
	 * @return true si el criterio debe aplicarse, false en caso contrario
	 */
	public boolean shouldApply() {
		try {
			return condition.get();
		} catch (Exception e) {
			// En caso de error en la evaluación, no aplicar el criterio
			return false;
		}
	}

	/**
	 * Combina este criterio con otro usando AND.
	 */
	public FilterCriteria and(FilterCriteria other) {
		return FilterCriteria.withCondition(this.sqlFragment + " AND " + other.sqlFragment, null, this.connector,
				() -> this.shouldApply() && other.shouldApply());
	}

	/**
	 * Combina este criterio con otro usando OR.
	 */
	public FilterCriteria or(FilterCriteria other) {
		return FilterCriteria.withCondition("(" + this.sqlFragment + " OR " + other.sqlFragment + ")", null,
				this.connector, () -> this.shouldApply() || other.shouldApply());
	}

	@Override
	public String toString() {
		return String.format("FilterCriteria{sqlFragment='%s', value=%s, connector='%s', shouldApply=%s}", sqlFragment,
				value, connector, shouldApply());
	}
}