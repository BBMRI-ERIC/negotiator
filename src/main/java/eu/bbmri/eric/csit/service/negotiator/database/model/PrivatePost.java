package eu.bbmri.eric.csit.service.negotiator.database.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Date;
import java.util.Set;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "private_post")
public class PrivatePost extends AuditEntity {

  @ManyToMany(mappedBy = "posts")
  @Exclude
  Set<Attachment> attachments;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id")
  @Exclude
  private Negotiation negotiation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "poster_id")
  @Exclude
  private Person person;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resource_id")
  @Exclude
  private Resource resource;

  @Lob
  private String postText;
  private String postStatus;
  private Date postDate;
}
