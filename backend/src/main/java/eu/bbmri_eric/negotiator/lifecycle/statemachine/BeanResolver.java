package eu.bbmri_eric.negotiator.lifecycle.statemachine;

/** Resolves a named bean (guard, action) at transition-build time. */
public interface BeanResolver {
  <T> T resolve(String beanName, Class<T> type);
}
