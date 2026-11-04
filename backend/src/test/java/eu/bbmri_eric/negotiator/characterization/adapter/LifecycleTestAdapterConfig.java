package eu.bbmri_eric.negotiator.characterization.adapter;

import eu.bbmri_eric.negotiator.governance.resource.ResourceService;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationLifecycleService;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceLifecycleService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Makes the {@link LifecycleTestAdapter} available to a characterization test.
 *
 * <p>Add {@code @Import(LifecycleTestAdapterConfig.class)} next to {@code @IntegrationTest} and
 * autowire {@link LifecycleTestAdapter}. As a {@code @TestConfiguration} it stays out of the
 * component scan, so it never reaches another test's context.
 */
@TestConfiguration(proxyBeanMethods = false)
public class LifecycleTestAdapterConfig {

  @Bean
  LifecycleTestAdapter lifecycleTestAdapter(
      NegotiationLifecycleService negotiationLifecycleService,
      ResourceLifecycleService resourceLifecycleService,
      NegotiationRepository negotiationRepository,
      ResourceService resourceService) {
    return new EnumBackedLifecycleTestAdapter(
        negotiationLifecycleService,
        resourceLifecycleService,
        negotiationRepository,
        resourceService);
  }
}
