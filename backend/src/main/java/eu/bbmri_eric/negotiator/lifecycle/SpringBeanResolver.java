package eu.bbmri_eric.negotiator.lifecycle;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.BeanResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SpringBeanResolver implements BeanResolver {

  private final ApplicationContext applicationContext;

  public SpringBeanResolver(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public <T> T resolve(String name, Class<T> type) {
    return applicationContext.getBean(name, type);
  }
}
