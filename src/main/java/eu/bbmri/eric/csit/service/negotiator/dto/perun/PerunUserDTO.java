package eu.bbmri.eric.csit.service.negotiator.dto.perun;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerunUserDTO {

  @NotBlank(message = "Perun user negotiation organization cannot be null or empty.")
  private String organization;

  @NotNull private Integer id;

  @NotBlank(message = "Perun user displayName cannot be null or empty.")
  private String displayName;

  @NotBlank(message = "Perun user status cannot be null or empty.")
  private String status;

  @NotBlank(message = "Perun user email cannot be null or empty.")
  private String mail;

  private String[] identities;
}
