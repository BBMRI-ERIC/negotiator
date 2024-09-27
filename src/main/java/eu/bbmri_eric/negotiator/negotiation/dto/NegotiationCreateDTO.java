package eu.bbmri_eric.negotiator.negotiation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import eu.bbmri_eric.negotiator.attachment.dto.AttachmentMetadataDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
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
public class NegotiationCreateDTO {

  @Valid @NotEmpty private String request;

  @NotNull private JsonNode payload;

  private Set<AttachmentMetadataDTO> attachments;
}
