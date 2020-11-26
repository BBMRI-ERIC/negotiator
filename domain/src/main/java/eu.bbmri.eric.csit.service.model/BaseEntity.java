package eu.bbmri.eric.csit.service.model;

import lombok.Getter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
// Choose your inheritance strategy:
//@Inheritance(strategy=InheritanceType.JOINED)
//@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public abstract class BaseEntity {
    @Id
    @GeneratedValue
    private Long id;
    private Date creation_date;
    private Date modified_date;
    private Long created_by;
    private Long modified_by;
}
