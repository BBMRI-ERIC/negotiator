package eu.bbmri.eric.csit.service.model;

import java.util.Date;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "post")

public class Post extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", insertable = false, updatable = false)
  private Person createdBy;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modified_by", insertable = false, updatable = false)
  private Person modifiedBy;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id")
  private Request request;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "poster_id")
  private Person poster;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "biobank_id")
  private Biobank biobank;
  @Lob
  private String postText;
  private String postStatus;
  private Date postDate;
  @ManyToMany(mappedBy = "posts")
  Set<Attachment> attachments;
}
