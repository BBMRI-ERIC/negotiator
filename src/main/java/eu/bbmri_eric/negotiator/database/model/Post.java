package eu.bbmri_eric.negotiator.database.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
@Table(name = "post")
public class Post extends AuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id")
  @Exclude
  private Negotiation negotiation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resource_id")
  @Exclude
  private Resource resource;

  @Column(columnDefinition = "TEXT")
  private String text;

  @Enumerated(EnumType.STRING)
  private PostStatus status;

  @Enumerated(EnumType.STRING)
  private PostType type;
}
