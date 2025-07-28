package eu.bbmri_eric.negotiator.user;

import org.springframework.data.jpa.domain.Specification;

/** This class provides specifications for querying the Person entity. */
public class PersonSpecifications {

  public static Specification<Person> fromUserFilters(UserFilterDTO filtersDTO) {
    Specification<Person> specs = null;
    if (filtersDTO.getId() != null) {
      specs = initOrAnd(specs, propertyEquals("id", filtersDTO.getId()));
    }

    if (filtersDTO.getName() != null) {
      specs = initOrAnd(specs, nameContains(filtersDTO.getName()));
    }

    if (filtersDTO.getEmail() != null) {
      specs = initOrAnd(specs, propertyEquals("email", filtersDTO.getEmail()));
    }

    if (filtersDTO.getSubjectId() != null) {
      specs = initOrAnd(specs, propertyEquals("subjectId", filtersDTO.getSubjectId()));
    }

    if (filtersDTO.getIsAdmin() != null) {
      specs = initOrAnd(specs, isAdmin(filtersDTO.getIsAdmin()));
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

  public static Specification<Person> nameContains(String substring) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.like(root.get("name"), "%" + substring + "%");
  }

  public static Specification<Person> isAdmin(Boolean isAdmin) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("admin"), isAdmin);
  }

  public static Specification<Person> propertyEquals(String property, String matchedString) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(property), matchedString);
  }
}
