package eu.bbmri_eric.negotiator.notification.email;

import java.util.ArrayList;

public interface EmailTemplateRepository {

  /**
   * Get all notification templates.
   *
   * @return the templates.
   */
  ArrayList<String> listAll();

  /**
   * Get a notification template.
   *
   * @param templateName the name of the template.
   * @return the template.
   */
  String load(String templateName);

  /**
   * Update a notification template.
   *
   * @param templateName the name of the template.
   * @param template the new template.
   */
  void save(String templateName, String template);

  /**
   * Reset a notification template.
   *
   * @param templateName the name of the template.
   */
  void reset(String templateName);
}
