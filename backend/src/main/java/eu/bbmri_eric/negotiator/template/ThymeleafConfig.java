package eu.bbmri_eric.negotiator.template;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Configuration for Thymeleaf template engine that enables database-stored templates to work with
 * Thymeleaf composition (fragments, includes, layouts).
 */
@Configuration
class ThymeleafConfig {

  private final DatabaseTemplateResolver databaseTemplateResolver;
  private final ApplicationContext applicationContext;

  ThymeleafConfig(
      DatabaseTemplateResolver databaseTemplateResolver, ApplicationContext applicationContext) {
    this.databaseTemplateResolver = databaseTemplateResolver;
    this.applicationContext = applicationContext;
  }

  @Bean
  public SpringTemplateEngine templateEngine() {
    SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    databaseTemplateResolver.setOrder(1);
    templateEngine.addTemplateResolver(databaseTemplateResolver);

    SpringResourceTemplateResolver classpathResolver = new SpringResourceTemplateResolver();
    classpathResolver.setApplicationContext(applicationContext);
    classpathResolver.setPrefix("classpath:/templates/");
    classpathResolver.setSuffix(".html");
    classpathResolver.setTemplateMode(TemplateMode.HTML);
    classpathResolver.setCharacterEncoding("UTF-8");
    classpathResolver.setOrder(2);
    classpathResolver.setCacheable(true);
    templateEngine.addTemplateResolver(classpathResolver);
    return templateEngine;
  }
}
