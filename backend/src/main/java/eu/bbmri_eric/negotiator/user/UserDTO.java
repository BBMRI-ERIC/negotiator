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
  private String id;

  @Schema(description = "Full name of the user", example = "John Doe")
  private String name;

  @Schema(description = "Email address of the user", example = "john.doe@example.com")
  private String email;

  /**
   * Creates a UserDTO from a UserResponseModel. Extracts only the basic information (id, name,
   * email).
   *
   * @param userResponseModel the source UserResponseModel
   * @return a new UserDTO with basic user information
   */
  public static UserDTO from(UserResponseModel userResponseModel) {
    return UserDTO.builder()
        .id(userResponseModel.getId())
        .name(userResponseModel.getName())
        .email(userResponseModel.getEmail())
        .build();
  }

  /**
   * Convenience method to set id from Long type. Useful when working with database entities that
   * use Long IDs.
   *
   * @param id the user id
   * @param name the user name
   * @param email the user email
   * @return a new UserDTO instance
   */
  public static UserDTO of(Long id, String name, String email) {
    return UserDTO.builder().id(String.valueOf(id)).name(name).email(email).build();
  }

  /**
   * Sets the ID from a Long value. Useful when working with database entities.
   *
   * @param id the Long ID to convert and set
   */
  public void setId(Long id) {
    this.id = String.valueOf(id);
  }
}
