package eu.bbmri_eric.negotiator.common.configuration;

import eu.bbmri_eric.negotiator.user.Person;
import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableJpaAuditing
@EntityScan(basePackages = {"eu.bbmri_eric.negotiator.*"})
@EnableJpaRepositories(basePackages = {"eu.bbmri_eric.negotiator.*"})
@EnableScheduling
@EnableAsync
@Configuration
public class BaseConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }

  @Bean
  public AuditorAware<Person> auditorProvider() {
    return new AuditorAwareImpl();
  }
}
