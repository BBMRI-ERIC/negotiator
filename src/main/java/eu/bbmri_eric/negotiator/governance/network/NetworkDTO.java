package eu.bbmri_eric.negotiator.governance.network;

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
  @NotNull private Long id;

  @NotNull private String externalId;

  @NotNull private String name;

  private String description;

  private String contactEmail;

  private String uri;
}
