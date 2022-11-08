package eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.negotiator.api.dto.project.ProjectCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.query.ResourceDTO;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class NegotiationRequestCreateDTO {

    @NotNull(message = "The url of the original query must be present")
    private String url;

    @NotNull(message = "A human readable description of the query must be present")
    private String humanReadable;

    @NotNull
    @NotEmpty(message = "At least one resource must be present")
    private Set<NegotiableEntityDTO> negotiableEntities;


}
