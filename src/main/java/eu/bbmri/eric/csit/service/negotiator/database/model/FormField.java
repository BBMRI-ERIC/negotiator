package eu.bbmri.eric.csit.service.negotiator.database.model;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
@Table(name = "form_field")
public class FormField extends BaseEntity {

  @NotNull
  private String name;

  @NotNull
  private String label;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "form_field_type_id")
  @Exclude
  private FormFieldType type;

  @NotNull private Boolean required;

  @OneToMany(mappedBy = "field")
  Set<FormFieldTemplateLink> templates;

//  @Nullable
//  private String category;

//  @Nullable
//  private String defaultValue;
//
//  @Column(name = "\"constraint\"")
//  private String constraint;
}
