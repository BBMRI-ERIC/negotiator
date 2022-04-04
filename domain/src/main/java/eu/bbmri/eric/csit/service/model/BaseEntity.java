package eu.bbmri.eric.csit.service.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import lombok.Getter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Entity
@Getter
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@TypeDefs({@TypeDef(name = "json", typeClass = JsonType.class)})
public abstract class BaseEntity {

  @Id @GeneratedValue private Long id;
}
