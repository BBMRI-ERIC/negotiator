package eu.bbmri_eric.negotiator.template;

import java.util.List;
import java.util.Map;

public interface TemplateService {

  /**
   * Get all templates for email notifications and PDF exports.
   *
   * @return the templates.
   */
  List<String> getAll();

  /**
   * Get a template.
   *
   * @param templateName the name of the template.
   * @return the template.
   */
  String getByName(String templateName);

  /**
   * Update a template.
   *
   * @param templateName the name of the template.
   * @param template the new template.
   */
  String updateTemplate(String templateName, String template);

  /**
   * Reset a template.
   *
   * @param templateName the name of the template.
   */
  String resetTemplate(String templateName);

  /**
   * Process a template with variables.
   *
   * @param variables the variables to substitute in the template.
   * @param templateName the name of the template.
   * @return the processed template string.
   */
  String processTemplate(Map<String, Object> variables, String templateName);
}
