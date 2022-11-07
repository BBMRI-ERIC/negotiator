package eu.bbmri.eric.csit.service.negotiator.database.model;

import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "form_field_type")
public class FormFieldType extends BaseEntity {

//  @ManyToOne(fetch = FetchType.LAZY)
//  @JoinColumn(name = "form_type_option_id")
//  @Exclude
//  private FormTypeOption formTypeOption;

  private String name;

//  private String validation;
}
