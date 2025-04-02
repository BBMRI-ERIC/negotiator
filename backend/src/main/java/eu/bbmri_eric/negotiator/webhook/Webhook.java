package eu.bbmri_eric.negotiator.webhook;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

  @OneToMany(mappedBy = "webhook", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("at DESC")
  private List<Delivery> deliveries = new ArrayList<>();

  public void addDelivery(Delivery delivery) {
    delivery.setWebhook(this);
    deliveries.add(0, delivery);
    if (deliveries.size() > 100) {
      deliveries.remove(deliveries.size() - 1);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Webhook webhook = (Webhook) o;
    return sslVerification == webhook.sslVerification
        && active == webhook.active
        && Objects.equals(id, webhook.id)
        && Objects.equals(url, webhook.url)
        && Objects.equals(deliveries, webhook.deliveries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, url, sslVerification, active, deliveries);
  }
}
