package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectorySyncJobRecord extends AuditEntity {

  @Id
  @GeneratedValue(generator = "uuid")
  @UuidGenerator
  @Column(name = "id")
  private String id;

  @Enumerated(EnumType.STRING)
  private DirectorySyncJobState jobState;

  private String jobException;
}
