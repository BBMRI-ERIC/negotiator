package eu.bbmri_eric.negotiator.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "webhook_secret")
public class WebhookSecret {

  @Id @GeneratedValue @UuidGenerator private String id;

  @Column(nullable = false)
  private String encryptedSecret;

  @Column(nullable = false)
  private String salt;

  public WebhookSecret(String encryptedSecret, String salt) {
    this.encryptedSecret = encryptedSecret;
    this.salt = salt;
  }
}
