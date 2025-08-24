package eu.bbmri_eric.negotiator.governance.resource.dto;

import eu.bbmri_eric.negotiator.user.UserDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceWithRepsDTO extends ResourceResponseModel {
  @Schema(
      description = "Representatives of this resource with their basic information",
      example =
          "[{\"id\": \"123\", \"name\": \"Sarah Rep\", \"email\": \"sarah@example.com\"}, {\"id\": \"456\", \"name\": \"Adam Rep\", \"email\": \"adam@example.com\"}]")
  private Set<UserDTO> representatives;
}
