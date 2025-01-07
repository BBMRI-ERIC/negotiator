package eu.bbmri_eric.negotiator.user;

import org.springframework.data.jpa.domain.Specification;

/** This class provides specifications for querying the Person entity. */
public class PersonSpecifications {
  public static Specification<Person> nameContains(String substring) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.like(root.get("name"), "%" + substring + "%");
  }

  public static Specification<Person> propertyEquals(String property, String matchedString) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(property), matchedString);
  }
}
