package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.governance.organization.dto.OrganizationFilterDTO;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class OrganizationSpecificationBuilder {

  public static Specification<Organization> build(OrganizationFilterDTO filters) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (StringUtils.hasText(filters.getName())) {
        predicates.add(
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get("name")),
                "%" + filters.getName().toLowerCase() + "%"));
      }

      if (StringUtils.hasText(filters.getExternalId())) {
        predicates.add(
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get("externalId")),
                "%" + filters.getExternalId().toLowerCase() + "%"));
      }

      if (filters.getWithdrawn() != null) {
        predicates.add(criteriaBuilder.equal(root.get("withdrawn"), filters.getWithdrawn()));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
