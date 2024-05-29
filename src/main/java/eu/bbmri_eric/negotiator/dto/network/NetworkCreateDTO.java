package eu.bbmri_eric.negotiator.dto.network;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NetworkCreateDTO {
  @NotNull private String externalId;

  private String name;

  private String contactEmail;

  private String uri;
}
