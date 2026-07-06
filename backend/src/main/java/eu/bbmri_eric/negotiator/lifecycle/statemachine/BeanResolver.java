package eu.bbmri_eric.negotiator.lifecycle.statemachine;

@FunctionalInterface
public interface BeanResolver {

  <T> T resolve(String name, Class<T> type);
}
