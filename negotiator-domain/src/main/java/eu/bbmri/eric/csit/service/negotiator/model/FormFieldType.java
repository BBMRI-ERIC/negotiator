package eu.bbmri.eric.csit.service.negotiator.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "form_field_type")
public class FormFieldType extends AuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "form_type_option_id")
  @Exclude
  private FormTypeOption formTypeOption;

  private String type;
  private String validation;
}
