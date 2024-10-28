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

/** Entity that represent a parameter for the configuration of the UI. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ui_parameter")
@ValidUIParameter
public class UIParameter {
  @Id Long id;

  /** The category of the parameter (e.g., footer, theme, ecc...) */
  @NotNull String category;

  /** The name of the parameter */
  @NotNull String name;

  /**
   * The type of the parameter. It may be STRING, BOOL, INT. It is needed to convert the value to
   * the proper type
   */
  @Enumerated(EnumType.STRING)
  @NotNull
  UIParameterType type;

  /** The value of the parameter as string */
  @NotNull String value;

  public Object getTypedValue() {
    return switch (getType()) {
      case INT -> Integer.valueOf(getValue());
      case BOOL -> Boolean.valueOf(getValue());
      default -> getValue();
    };
  }
}
