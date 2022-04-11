package eu.bbmri.eric.csit.service.negotiator.dto.request;

import javax.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

public class CollectionDTO {

  @NotNull private String id;

  @Nullable private String name;

  public String getId() {
    return id;
  }

  @Nullable
  public String getName() {
    return name;
  }
}
