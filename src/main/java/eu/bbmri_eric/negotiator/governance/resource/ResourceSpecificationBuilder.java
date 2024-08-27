package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.common.FilterDTO;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class ResourceSpecificationBuilder {

  public static Specification<Resource> build(FilterDTO filterDTO) {
    return (Root<Resource> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      // Iterate over all fields of the DTO
      for (Field field : filterDTO.getClass().getDeclaredFields()) {
        try {
          field.setAccessible(true);
          Object value = field.get(filterDTO);

          // Add predicate if the field is not null and is not a paging field
          if (value != null && !isPagingField(field.getName())) {
            String fieldName = field.getName();

            // Example: String field like condition
            if (value instanceof String v) {
              predicates.add(
                  criteriaBuilder.like(
                      criteriaBuilder.lower(root.get(fieldName)), "%" + v.toLowerCase() + "%"));
            }
            // Add more type conditions as needed (e.g., for numbers, dates, etc.)
            else {
              predicates.add(criteriaBuilder.equal(root.get(fieldName), value));
            }
          }
        } catch (IllegalAccessException e) {
          // Handle exception or log it
        }
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  // Helper method to ignore paging fields
  private static boolean isPagingField(String fieldName) {
    return "page".equals(fieldName) || "size".equals(fieldName);
  }
}
