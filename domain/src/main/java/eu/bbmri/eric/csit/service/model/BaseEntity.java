package eu.bbmri.eric.csit.service.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
// Choose your inheritance strategy:
//@Inheritance(strategy=InheritanceType.JOINED)
//@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonType.class)
})
public abstract class BaseEntity {
    @Id
    @GeneratedValue
    private Long id;
    private Date creation_date;
    private Date modified_date;
    private Long created_by;
    private Long modified_by;
}
