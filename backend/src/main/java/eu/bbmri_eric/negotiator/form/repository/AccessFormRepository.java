package eu.bbmri_eric.negotiator.form.repository;

import eu.bbmri_eric.negotiator.form.AccessForm;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessFormRepository extends JpaRepository<AccessForm, Long> {

  @NotNull
  Optional<AccessForm> findById(Long id);

  @Query(
      value =
          "SELECT DISTINCT a "
              + "FROM AccessForm a "
              + "JOIN FETCH a.resources r "
              + "WHERE r.sourceId = :entityId")
  Optional<AccessForm> findByResourceId(String entityId);

  @Query(
      value =
          "SELECT EXISTS ( "
              + "SELECT DISTINCT a.id "
              + "FROM access_form_section_link a "
              + "WHERE a.access_form_id = :accessFormId AND a.access_form_section_id = :sectionId"
              + ")",
      nativeQuery = true)
  boolean isSectionPartOfAccessForm(Long accessFormId, Long sectionId);

  @Query(
      value =
          "SELECT EXISTS ( "
              + "SELECT DISTINCT * "
              + "FROM access_form_section_element_link afsel JOIN access_form_section_link afsl ON afsel.access_form_section_link_id = afsl.id "
              + "WHERE afsel.access_form_element_id = :elementId "
              + "AND afsl.access_form_section_id = :sectionId "
              + "AND afsl.access_form_id = :accessFormId "
              + ")",
      nativeQuery = true)
  boolean isElementPartOfSectionOfAccessForm(Long accessFormId, Long sectionId, Long elementId);
}
