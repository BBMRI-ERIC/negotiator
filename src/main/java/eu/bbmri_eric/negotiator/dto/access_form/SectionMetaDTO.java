package eu.bbmri_eric.negotiator.dto.access_form;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Relation(collectionRelation = "sections", itemRelation = "section")
public class SectionMetaDTO {
  @NotNull private Long id;
  @NotNull private String name;

  @NotNull private String label;

  @NotNull private String description;
}
