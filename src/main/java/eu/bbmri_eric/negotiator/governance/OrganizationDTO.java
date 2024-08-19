package eu.bbmri_eric.negotiator.governance;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Relation(collectionRelation = "organizations", itemRelation = "organization")
public class OrganizationDTO {
  @NotNull private Long id;
  @NotNull private String externalId;
  private String name;
}
