package eu.bbmri_eric.negotiator.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Relation(collectionRelation = "networks", itemRelation = "network")
public class NetworkDTO {
  private Long id;

  @NotNull private String externalId;

  private String name;

  private String contactEmail;

  private String uri;
}
