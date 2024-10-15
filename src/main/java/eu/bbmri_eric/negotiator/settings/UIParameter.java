package eu.bbmri_eric.negotiator.settings;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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
@ValidUIParameter
public class UIParameter {
  @Id Long id;

  @NotNull String category;

  @NotNull String name;

  @Enumerated(EnumType.STRING)
  @NotNull
  UIParameterType type;

  @NotNull String value;
}
