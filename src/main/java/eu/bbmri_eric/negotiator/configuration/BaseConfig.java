package eu.bbmri_eric.negotiator.configuration;

import eu.bbmri_eric.negotiator.database.model.Network;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.dto.network.NetworkCreateDTO;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableJpaAuditing
public class BaseConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public ModelMapper modelMapper() {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    PropertyMap<NetworkCreateDTO, Network> networkMap =
        new PropertyMap<NetworkCreateDTO, Network>() {
          @Override
          protected void configure() {
            skip(destination.getId());
          }
        };
    modelMapper.addMappings(networkMap);

    return modelMapper;
  }

  @Bean
  public AuditorAware<Person> auditorProvider() {
    return new AuditorAwareImpl();
  }
}
