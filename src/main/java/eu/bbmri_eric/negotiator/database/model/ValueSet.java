package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(name = "value_set_id_seq", initialValue = 100)
public class ValueSet {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "value_set_id_seq")
  private Long id;

  private String name;
  private String externalDocumentation;
  @ElementCollection private List<String> availableValues = new ArrayList<>();

  public ValueSet(String name, String externalDocumentation, List<String> availableValues) {
    this.name = name;
    this.externalDocumentation = externalDocumentation;
    this.availableValues = availableValues;
  }
}
