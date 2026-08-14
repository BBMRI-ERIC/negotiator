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
 * autowire {@link LifecycleTestAdapter}. It is a {@code @TestConfiguration}, so it is excluded from
 * the application's component scan and cannot leak into the context of any other test.
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
