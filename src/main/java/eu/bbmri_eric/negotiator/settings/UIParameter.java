package eu.bbmri_eric.negotiator.settings;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ui_parameter")
public class UIParameter {
  @Id Long id;

  String category;

  String name;

  @Enumerated(EnumType.STRING)
  UIParameterType type;

  String value;
}
