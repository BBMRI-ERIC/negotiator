package eu.bbmri_eric.negotiator.email;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEmail {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String address;

  @Column(columnDefinition = "TEXT")
  private String message;

  private LocalDateTime sentAt = LocalDateTime.now();

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    NotificationEmail that = (NotificationEmail) o;
    return Objects.equals(id, that.id)
        && Objects.equals(address, that.address)
        && Objects.equals(message, that.message)
        && Objects.equals(sentAt, that.sentAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, address, message, sentAt);
  }
}
