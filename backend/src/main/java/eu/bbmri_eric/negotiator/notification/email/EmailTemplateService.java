package eu.bbmri_eric.negotiator.notification.email;

public interface EmailTemplateService {

    /**
     * Get a notification template.
     *
     * @param templateName the name of the template.
     * @return the template.
     */
    String getNotificationTemplate(String templateName);

    /**
     * Update a notification template.
     *
     * @param templateName the name of the template.
     * @param template the new template.
     */
    String updateNotificationTemplate(String templateName, String template);

    /**
     * Reset a notification template.
     *
     * @param templateName the name of the template.
     */
    String resetNotificationTemplate(String templateName);
}
