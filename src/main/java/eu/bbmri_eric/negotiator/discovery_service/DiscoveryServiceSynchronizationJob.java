package eu.bbmri_eric.negotiator.discovery_service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity(name = "DiscoveryServiceSynchronizationJob")
public class DiscoveryServiceSynchronizationJob {

  @Id
  @GeneratedValue(generator = "uuid")
  @UuidGenerator
  @Column(name = "id")
  private String id;

  @ManyToOne
  @JoinColumn(name = "discovery_service_id", referencedColumnName = "id")
  private DiscoveryService service;

  private LocalDateTime creationDate;

  private LocalDateTime modifiedDate;

  @Enumerated(EnumType.STRING)
  private DiscoveryServiceSyncronizationJobStatus status;
}
