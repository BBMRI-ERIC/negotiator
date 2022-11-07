package eu.bbmri.eric.csit.service.negotiator.database.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
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
@Table(name = "form_field_template_link")
@IdClass(FormFieldTemplateId.class)
public class FormFieldTemplateLink {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id")
  @Exclude
  @Id
  private FormTemplate template;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "field_id")
  @Exclude
  @Id
  private FormField field;

  @NotNull
  private Integer ordering;
}
