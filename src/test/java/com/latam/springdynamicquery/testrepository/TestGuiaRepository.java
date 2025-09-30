package com.latam.springdynamicquery.testrepository;

import com.latam.springdynamicquery.core.criteria.FilterCriteria;
import com.latam.springdynamicquery.repository.DynamicRepository;
import com.latam.springdynamicquery.testmodel.GuiaDespacho;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository de prueba para GuiaDespacho (tu caso específico).
 */
@Repository
public interface TestGuiaRepository extends DynamicRepository<GuiaDespacho, Long> {
    
    // Método específico para tu caso: búsqueda con OR y EXISTS
    default List<GuiaDespacho> findGuiasByNumero(String numero) {
        if (numero == null || numero.trim().isEmpty()) {
            return List.of();
        }
        
        Map<String, FilterCriteria> filters = Map.of(
            "numeroGuia", FilterCriteria.whenNotEmpty(
                "(gd.numero_guia LIKE :numeroGuia OR EXISTS(SELECT 1 FROM numeroawb NA WHERE NA.id = gd.id AND NA.numero LIKE :numeroGuia))",
                "%" + numero + "%"
            )
        );
        
        return executeNamedQuery("GuiaMapper.findGuiasByNumero", filters);
    }
    
    default Optional<GuiaDespacho> findGuiaByIdFromYaml(Long id) {
        if (id == null) return Optional.empty();
        
        Map<String, FilterCriteria> filters = Map.of(
            "id", FilterCriteria.when("", id)
        );
        
        List<GuiaDespacho> guias = executeNamedQuery("GuiaMapper.findGuiaById", filters);
        return guias.isEmpty() ? Optional.empty() : Optional.of(guias.get(0));
    }
    
    default List<GuiaDespacho> findGuiasAdvanced(String numero, String estado, Long clienteId, 
                                               LocalDate fechaInicio, LocalDate fechaFin) {
        Map<String, FilterCriteria> filters = Map.of(
            "numeroGuia", FilterCriteria.whenNotEmpty(
                "(gd.numero_guia LIKE :numeroGuia OR EXISTS(SELECT 1 FROM numeroawb NA WHERE NA.id = gd.id AND NA.numero LIKE :numeroGuia))",
                "%" + numero + "%"
            ),
            "estado", FilterCriteria.whenNotEmpty("gd.estado = :estado", estado),
            "clienteId", FilterCriteria.whenNumericPositive("gd.cliente_id = :clienteId", clienteId),
            "fechaInicio", FilterCriteria.whenNotNull("gd.fecha_creacion >= :fechaInicio", fechaInicio),
            "fechaFin", FilterCriteria.whenNotNull("gd.fecha_creacion <= :fechaFin", fechaFin)
        );
        
        return executeNamedQuery("GuiaMapper.findGuiasAdvanced", filters);
    }
}