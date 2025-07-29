package eu.bbmri_eric.negotiator.user;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/** This class provides specifications for querying the Person entity. */
public class PersonSpecifications {

  public static Specification<Person> fromUserFilters(UserFilterDTO filtersDTO) {
    Specification<Person> specs = null;
    if (filtersDTO.getId() != null) {
      specs = initOrAnd(specs, propertyEquals("id", filtersDTO.getId()));
    }

    if (filtersDTO.getName() != null) {
      specs = initOrAnd(specs, propertyContains("name", filtersDTO.getName()));
    }

    if (filtersDTO.getEmail() != null) {
      specs = initOrAnd(specs, propertyContains("email", filtersDTO.getEmail()));
    }

    if (filtersDTO.getSubjectId() != null) {
      specs = initOrAnd(specs, propertyContains("subjectId", filtersDTO.getSubjectId()));
    }

    if (filtersDTO.getIsAdmin() != null) {
      specs = initOrAnd(specs, isAdmin(filtersDTO.getIsAdmin()));
    }

    if (filtersDTO.getLastLoginAfter() != null || filtersDTO.getLastLoginBefore() != null) {
      specs =
          initOrAnd(
              specs,
              createdBetween(filtersDTO.getLastLoginAfter(), filtersDTO.getLastLoginBefore()));
    }

    return specs;
  }

  private static Specification<Person> initOrAnd(
      Specification<Person> overallSpec, Specification<Person> newSpec) {
    if (overallSpec == null) {
      return newSpec;
    } else {
      return overallSpec.and(newSpec);
    }
  }

  public static Specification<Person> isAdmin(Boolean isAdmin) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("admin"), isAdmin);
  }

  public static Specification<Person> propertyEquals(String property, String matchedString) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(property), matchedString);
  }

  public static Specification<Person> propertyContains(String property, String substring) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.like(
            criteriaBuilder.lower(root.get(property)), "%" + substring.toLowerCase() + "%");
  }

  /**
   * Condition to filter by lastLoginDate. Both lower and upper bound date may be avoided
   *
   * @param after the LocalDate after which the Person logged in last time was created
   * @param before the LocalDate before which the Person logged in last time was created
   * @return a Specification to add as part of a query to filter Persons
   */
  public static Specification<Person> createdBetween(LocalDate after, LocalDate before) {
    return (root, query, criteriaBuilder) -> {
      if (after == null && before == null) { // return void condition
        return criteriaBuilder.conjunction();
      } else if (after != null && before != null) {
        return criteriaBuilder.between(root.get("lastLogin"), after, before);
      } else if (after != null) {
        return criteriaBuilder.greaterThan(root.get("lastLogin"), after);
      } else {
        return criteriaBuilder.lessThan(root.get("lastLogin"), before);
      }
    };
  }
}
