package eu.bbmri_eric.negotiator.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class PostCreateDTO {

  @Valid @NotEmpty private String text;

  @Valid private String organizationId;

  @Valid private PostType type;

  @Schema(
      description =
          "Display name of the help desk actor who created this post (e.g. the representative's name or email). "
              + "Only accepted from callers holding ROLE_HELPDESK_INTEGRATION. Stored for display purposes only.",
      example = "john.smith@mhh.de")
  private String helpdeskActor;
}
