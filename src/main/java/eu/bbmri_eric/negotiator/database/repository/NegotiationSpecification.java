package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Resource;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

public class NegotiationSpecification {

  /**
   * Condition to filter Negotiation by states
   *
   * @param states a List of NegotiationState to use as filter
   * @return a Specification to add as part of a query to filter Negotiations
   */
  public static Specification<Negotiation> hasState(List<NegotiationState> states) {
    return new Specification<>() {
      List<NegotiationState> inputStates;
      @Nullable
      @Override
      public Predicate toPredicate(
          @Nonnull Root<Negotiation> root,
          @Nonnull CriteriaQuery<?> query,
          @Nonnull CriteriaBuilder criteriaBuilder) {
        inputStates = states;
        if (states.size() == 1) {
          return criteriaBuilder.equal(root.get("currentState"), states.get(0));
        } else {
          return criteriaBuilder.in(root.get("currentState")).value(states);
        }
      }
      @Override
      public String toString() {
        return inputStates.toString();
      }
    };
  }

  /**
   * Condition to filter Negotiation by author (i.e., createdBy)
   *
   * @param person the Person that created the negotiation
   * @return a Specification to add as part of a query to filter Negotiations
   */
  public static Specification<Negotiation> hasAuthor(Person person) {
    return new Specification<>() {
      @Nullable
      @Override
      public Predicate toPredicate(
          @Nonnull Root<Negotiation> root,
          @Nonnull CriteriaQuery<?> query,
          @Nonnull CriteriaBuilder criteriaBuilder) {
        return criteriaBuilder.equal(root.get("createdBy"), person);
      }
    };
  }

  /**
   * Condition to filter Negotiation by the Resources involved
   *
   * @param resources the Person that created the negotiation
   * @return a Specification to add as part of a query to filter Negotiations
   */
  public static Specification<Negotiation> hasResourcesIn(Set<Resource> resources) {
    return new Specification<>() {
      @Nullable
      @Override
      public Predicate toPredicate(
          @Nonnull Root<Negotiation> root,
          @Nonnull CriteriaQuery<?> query,
          @Nonnull CriteriaBuilder criteriaBuilder) {
        return root.join("requests").joinSet("resources").in(resources);
      }
    };
  }

  public static Specification<Negotiation> hasTimeRange(LocalDate startDate, LocalDate endDate) {
    return new Specification<>() {
      @Nullable
      @Override
      public Predicate toPredicate(
          @Nonnull Root<Negotiation> root,
          @Nonnull CriteriaQuery<?> query,
          @Nonnull CriteriaBuilder criteriaBuilder) {
        if (startDate == null && endDate == null) {
          return criteriaBuilder.conjunction();
        } else if (startDate != null && endDate != null) {
          return criteriaBuilder.between(root.get("creationDate"), startDate, endDate);
        } else if (startDate != null) {
          return criteriaBuilder.greaterThan(root.get("creationDate"), startDate);
        } else {
          return criteriaBuilder.lessThan(root.get("creationDate"), endDate);
        }
      }
    };
  }
}
