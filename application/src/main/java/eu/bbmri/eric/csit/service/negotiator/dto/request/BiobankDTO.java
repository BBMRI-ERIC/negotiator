package eu.bbmri.eric.csit.service.negotiator.dto.request;

import eu.bbmri.eric.csit.service.negotiator.dto.request.CollectionDTO;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

public class BiobankDTO {

  @NotNull private String id;

  @Nullable private String name;

  @Nullable private List<CollectionDTO> collections;

  public String getId() {
    return id;
  }

  @Nullable
  public String getName() {
    return name;
  }

  @Nullable
  public List<CollectionDTO> getCollections() {
    return collections;
  }
}
