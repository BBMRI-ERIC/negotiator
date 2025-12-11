package eu.bbmri_eric.negotiator.settings;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity used to set global admin settings, that are the same for all administrators in a specific
 * Negotiator's installation.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "admin_settings")
@ValidUIParameter
public class AdminSettings {

  /**
   * Flag to send or not the email notifications of every update in a negotiation to all the
   * Administrators. If set tu true, all the administrator will be notified for a change on every
   * ongoing negotiation in the specific Negotiator's installation.
   */
  @Id private Long id = 1L;

  private boolean sendNegotiationUpdatesNotifications;
}
