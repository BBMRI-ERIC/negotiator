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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Date;

@ToString
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "notification_mail")
public class NotificationMail extends AuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notification_id")
  @Exclude
  private Notification notification;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipient_id")
  @Exclude
  private Person person;

  private String emailAddress;
  private String sendStatus;
  private Date sendDate;
  private String subject;
  @Lob
  private String body;
}
