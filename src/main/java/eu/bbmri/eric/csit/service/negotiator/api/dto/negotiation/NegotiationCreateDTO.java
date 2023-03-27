package eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class NegotiationCreateDTO {

  @Valid
  @NotEmpty
  private Set<String> requests;

  @NotNull
  private String payload;
}
