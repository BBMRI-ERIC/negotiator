package eu.bbmri_eric.negotiator.webhook;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Webhook {

  // Getters and Setters
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String url;

  // Indicates whether SSL certificate verification is enabled
  private boolean sslVerification;

  private boolean active;

  protected Webhook() {}

  public Webhook(String url, boolean sslVerification, boolean active) {
    this.url = url;
    this.sslVerification = sslVerification;
    this.active = active;
  }
}
