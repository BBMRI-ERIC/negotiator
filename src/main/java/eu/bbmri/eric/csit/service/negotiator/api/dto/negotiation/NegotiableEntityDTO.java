package eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.negotiator.api.dto.query.ResourceDTO;
import lombok.*;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class NegotiableEntityDTO {
    @NotNull
    private String entity_identifier;

    @Nullable
    private String name;

    @Nullable
    private Set<NegotiableEntityDTO> children;
}
