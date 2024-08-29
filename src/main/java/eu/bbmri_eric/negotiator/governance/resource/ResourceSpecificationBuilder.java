package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceFilterDTO;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Builder for a Resource specification/query. It builds a specification based on the filter DTO,
 * any non-null fields in the filter DTO will be used as filters.
 */
@Component
@CommonsLog
public class ResourceSpecificationBuilder {

  public static Specification<Resource> build(ResourceFilterDTO filterDTO) {
    return (Root<Resource> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      for (Field field : filterDTO.getClass().getDeclaredFields()) {
        try {
          field.setAccessible(true);
          Object value = field.get(filterDTO);

          if (value != null && !isPagingField(field.getName())) {
            String fieldName = field.getName();
            if (value instanceof String v) {
              predicates.add(
                  criteriaBuilder.like(
                      criteriaBuilder.lower(root.get(fieldName)), "%" + v.toLowerCase() + "%"));
            } else {
              predicates.add(criteriaBuilder.equal(root.get(fieldName), value));
            }
          }
        } catch (IllegalAccessException e) {
          log.error("Error while building specification", e);
        }
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
  
  private static boolean isPagingField(String fieldName) {
    return "page".equals(fieldName) || "size".equals(fieldName);
  }
}
