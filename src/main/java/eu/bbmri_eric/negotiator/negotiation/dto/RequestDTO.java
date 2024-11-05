package eu.bbmri_eric.negotiator.negotiation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Schema(
    description = "Data Transfer Object for API requests",
    example =
        """
                 {
                   "id": "3da96693-19da-4837-97f0-27fb57265807",
                   "url": "https://bbmritestnn.gcc.rug.nl",
                   "humanReadable": "#1: No filters used",
                   "resources": [
                     {
                       "id": "bbmri-eric:ID:CZ_MMCI:collection:LTS",
                       "name": "Test collection 2",
                       "organization": {
                         "id": 2,
                         "externalId": "bbmri-eric:ID:CZ_MMCI",
                         "name": "Masaryk Memorial Cancer Institute"
                       }
                     }
                   ],
                   "redirectUrl": "http://localhost:8080/requests/3da96693-19da-4837-97f0-27fb57265807"
                 }
                 """)
public class RequestDTO {

  @NotNull private String id;

  @NotNull(message = "The url of the original request must be present")
  private String url;

  @NotNull(message = "A human readable description of the request must be present")
  private String humanReadable;

  @NotNull
  @NotEmpty(message = "At least one resource must be present")
  @Schema(description = "List of Resources that the user is applying for access to")
  private Set<ResourceDTO> resources;

  @NotNull
  @Schema(
      description =
          "URL to which the user should be redirected so he can fill out the access application")
  private String redirectUrl;

  private String negotiationId;
}
