package eu.bbmri.eric.csit.service.negotiator.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class BiobankDTO {

  @NotNull private String id;

  @Nullable private String name;

  @Nullable private List<CollectionDTO> collections;
}
