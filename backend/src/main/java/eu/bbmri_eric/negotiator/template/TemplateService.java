package eu.bbmri_eric.negotiator.template;

import java.util.List;

interface TemplateService {

  /**
   * Get all notification templates.
   *
   * @return the templates.
   */
  List<String> getAll();

  /**
   * Get a notification template.
   *
   * @param templateName the name of the template.
   * @return the template.
   */
  String getByName(String templateName);

  /**
   * Update a notification template.
   *
   * @param templateName the name of the template.
   * @param template the new template.
   */
  String updateTemplate(String templateName, String template);

  /**
   * Reset a notification template.
   *
   * @param templateName the name of the template.
   */
  String resetTemplate(String templateName);
}
