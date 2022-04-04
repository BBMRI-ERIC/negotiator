package eu.bbmri.eric.csit.service.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
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
@Table(name = "form_field_selection")
public class FormFieldSelection extends AuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  @Exclude
  private Person createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modified_by", insertable = false, updatable = false)
  @Exclude
  private Person modifiedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "form_template_id")
  @Exclude
  private FormTemplate formTemplate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "form_field_id")
  @Exclude
  private FormField formField;

  @Column(name = "\"order\"")
  private Integer order;
}
