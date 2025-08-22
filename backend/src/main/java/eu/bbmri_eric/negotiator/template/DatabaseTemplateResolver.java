package eu.bbmri_eric.negotiator.template;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

/**
 * Custom Thymeleaf template resolver that fetches templates from the database instead of the
 * filesystem. This enables Thymeleaf composition (fragments, includes) to work with database-stored
 * templates.
 */
@Component
class DatabaseTemplateResolver extends AbstractConfigurableTemplateResolver {

  private final TemplateRepository templateRepository;

  DatabaseTemplateResolver(TemplateRepository templateRepository) {
    super();
    this.templateRepository = templateRepository;
    setOrder(1);
    setTemplateMode(TemplateMode.HTML);
    setCacheable(false); // Always get fresh data from database
  }

  @Override
  protected ITemplateResource computeTemplateResource(
      IEngineConfiguration configuration,
      String ownerTemplate,
      String template,
      String resourceName,
      String characterEncoding,
      Map<String, Object> templateResolutionAttributes) {

    // Try to find the template in the database
    return templateRepository
        .findByName(template)
        .map(dbTemplate -> (ITemplateResource) new StringTemplateResource(dbTemplate.getContent()))
        .orElse(null); // Template not found - let other resolvers try
  }
}
