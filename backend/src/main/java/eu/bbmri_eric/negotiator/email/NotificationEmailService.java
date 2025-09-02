package eu.bbmri_eric.negotiator.email;

import org.springframework.data.domain.Page;

interface NotificationEmailService {

  /**
   * Retrieves all notification emails with optional filtering, pagination and sorting.
   *
   * @param filters NotificationEmailFilterDTO containing all filter and pagination parameters
   * @return Page of NotificationEmailDTO objects
   */
  Page<NotificationEmailDTO> findAllWithFilters(NotificationEmailFilterDTO filters);

  /**
   * Retrieves a notification email by its ID.
   *
   * @param id the ID of the notification email
   * @return NotificationEmailDTO object
   */
  NotificationEmailDTO findById(Long id);
}
