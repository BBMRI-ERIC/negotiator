package eu.bbmri_eric.negotiator.user;

import eu.bbmri_eric.negotiator.common.FilterDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Parameters to filter the users")
public class UserFilterDTO implements FilterDTO {
  @Schema(description = "The name of the user")
  String name;

  @Schema(description = "The email of the user")
  String email;

  @Schema(description = "The subjectId of the user")
  String subjectId;

  @Schema(description = "Whether the user is admin or not")
  Boolean isAdmin;

  @Schema(
      description = "The field to use to sort the results",
      example = "name",
      defaultValue = "lastLogin")
  UserSortField sortBy = UserSortField.lastLogin;

  @Schema(description = "The direction of the sorting", example = "DESC")
  Sort.Direction sortOrder = Sort.Direction.DESC;

  @Schema(description = "The page number required", example = "0")
  @Min(value = 0, message = "Page number must be greater than or equal to 0")
  int page = 0;

  @Schema(description = "The size of the pages required", example = "50")
  @Min(value = 1, message = "Page size must be greater than or equal to 1")
  int size = 50;
}
