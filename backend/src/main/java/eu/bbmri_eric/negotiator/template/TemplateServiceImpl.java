package eu.bbmri_eric.negotiator.template;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TemplateServiceImpl implements TemplateService {
  @Override
  public List<String> getAllNotificationTemplates() {
    return List.of();
  }

  @Override
  public String getNotificationTemplate(String templateName) {
    return "";
  }

  @Override
  public String updateNotificationTemplate(String templateName, String template) {
    return "";
  }

  @Override
  public String resetNotificationTemplate(String templateName) {
    return "";
  }
}
