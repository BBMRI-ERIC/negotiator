package eu.bbmri_eric.negotiator.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminSettingsRepository
    extends JpaRepository<AdminSettings, Long>, JpaSpecificationExecutor<AdminSettings> {

  @Query(
      value =
          "select s.send_negotiations_update_notifications from public.admin_settings s where s.id = 1",
      nativeQuery = true)
  boolean getSendNegotiationUpdatesNotifications();
}
