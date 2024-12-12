package eu.bbmri_eric.negotiator.governance.organization;


import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Relation(collectionRelation = "organizations", itemRelation = "organization")
public class OrganizationDetailDTO {
    @NotNull
    private Long id;
    @NotNull private String externalId;
    private String name;
    private List<ResourceResponseModel> resources;

}
