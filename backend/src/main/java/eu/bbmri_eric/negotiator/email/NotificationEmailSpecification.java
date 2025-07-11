package eu.bbmri_eric.negotiator.email;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class NotificationEmailSpecification {

  public static Specification<NotificationEmail> withFilters(
      String address, LocalDateTime sentAfter, LocalDateTime sentBefore) {

    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (StringUtils.hasText(address)) {
        predicates.add(
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get("address")),
                criteriaBuilder.lower(criteriaBuilder.literal("%" + address + "%"))));
      }

      if (sentAfter != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("sentAt"), sentAfter));
      }

      if (sentBefore != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("sentAt"), sentBefore));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
