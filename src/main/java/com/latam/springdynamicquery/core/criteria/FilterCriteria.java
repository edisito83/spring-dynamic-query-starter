package com.latam.springdynamicquery.core.criteria;

import com.latam.springdynamicquery.core.validation.ValidationUtils;
import lombok.Getter;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Representa un criterio de filtrado dinámico para consultas SQL. Permite
 * construir condiciones WHERE dinámicamente basadas en la presencia y validez
 * de valores.
 */
@Getter
public class FilterCriteria {

	private final String sqlFragment;
	private final Object value;
	private final String connector;
	private final Supplier<Boolean> condition;

	/**
	 * Constructor principal para FilterCriteria.
	 * 
	 * @param sqlFragment El fragmento SQL a agregar si la condición se cumple
	 * @param value       El valor del parámetro
	 * @param connector   El conector SQL (AND, OR)
	 * @param condition   La condición que determina si aplicar este criterio
	 */
	public FilterCriteria(String sqlFragment, Object value, String connector, Supplier<Boolean> condition) {
		this.sqlFragment = sqlFragment;
		this.value = value;
		this.connector = connector != null ? connector : "AND";
		this.condition = condition != null ? condition : () -> ValidationUtils.isValidValue(value);
	}

	/**
	 * Constructor simplificado con conector especificado.
	 */
	public FilterCriteria(String sqlFragment, Object value, String connector) {
		this(sqlFragment, value, connector, null);
	}

	/**
	 * Constructor más simple con conector AND por defecto.
	 */
	public FilterCriteria(String sqlFragment, Object value) {
		this(sqlFragment, value, "AND", null);
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
	 * Crea un criterio que se aplica cuando el valor no está vacío. Funciona con
	 * strings y colecciones.
	 */
	public static FilterCriteria whenNotEmpty(String sqlFragment, Object value) {
		return new FilterCriteria(sqlFragment, value, "AND", () -> switch (value) {
		case null -> false;
		case String s -> !s.trim().isEmpty();
		case Collection<?> c -> !c.isEmpty();
		default -> true;
		});
	}

	/**
	 * Crea un criterio que se aplica cuando el valor es un número positivo.
	 */
	public static FilterCriteria whenNumericPositive(String sqlFragment, Object value) {
		return new FilterCriteria(sqlFragment, value, "AND", () -> ValidationUtils.isValidNumericValue(value));
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