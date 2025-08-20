package eu.bbmri_eric.negotiator.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A simple DTO class for basic user information. Contains only essential user data: id, name, and
 * email.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Basic user information transfer object")
public class UserDTO {

  @Schema(description = "Unique identifier of the user", example = "123")
  private Long id;

  @Schema(description = "Full name of the user", example = "John Doe")
  private String name;

  @Schema(description = "Email address of the user", example = "john.doe@example.com")
  private String email;
}
