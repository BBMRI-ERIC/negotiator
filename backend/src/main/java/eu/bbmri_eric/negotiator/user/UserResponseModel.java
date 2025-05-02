package eu.bbmri_eric.negotiator.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

/** A DTO model for a person authenticated via OAuth. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response model representing a user in the system")
@Relation(collectionRelation = "users", itemRelation = "user")
public class UserResponseModel {
  @Schema(description = "Unique identifier of the user", example = "123")
  private String id;

  @Schema(
      description = "Subject ID of the user, typically an external identifier",
      example = "user-123@oidc-provider.com")
  private String subjectId;

  @Schema(description = "Full name of the user", example = "John Doe")
  private String name;

  @Schema(description = "Email address of the user", example = "john.doe@example.com")
  private String email;

  @Schema(description = "Indicates whether the user represents any resource", example = "true")
  private boolean isRepresentativeOfAnyResource;

  @Schema(description = "Indicates whether the user is an admin", example = "false")
  private boolean isAdmin;

  @Schema(description = "Indicates whether the user is a network manager", example = "true")
  private boolean isNetworkManager;

  @Schema(description = "Timestamp of the last successful login", example = "2021-01-01T12:00:00Z")
  private LocalDateTime lastLogin;

  public void setId(Long id) {
    this.id = String.valueOf(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserResponseModel userModel = (UserResponseModel) o;
    return Objects.equals(id, userModel.id)
        && Objects.equals(subjectId, userModel.subjectId)
        && Objects.equals(name, userModel.name)
        && Objects.equals(email, userModel.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, subjectId, name, email);
  }
}
