package eu.bbmri_eric.negotiator.governance.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceWithRepsDTO extends ResourceResponseModel {
  @Schema(
      description = "Names of representatives",
      example = "[\"Sarah Rep\", \"Adam Rep\", \"John Rep\"]")
  private Set<String> representatives;
}
