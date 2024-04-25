package eu.bbmri_eric.negotiator.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

/** DTO for {@link eu.bbmri_eric.negotiator.database.model.ValueSet} */
@Value
@Getter
@Setter
@AllArgsConstructor
public class ValueSetDto implements Serializable {
  Long id;
  String name;
  String externalDocumentation;
  List<String> availableValues;
}
