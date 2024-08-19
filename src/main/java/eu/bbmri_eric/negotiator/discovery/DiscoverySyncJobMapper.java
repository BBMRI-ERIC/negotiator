package eu.bbmri_eric.negotiator.discovery;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class DiscoverySyncJobMapper {
  @Autowired ModelMapper modelMapper;

  @PostConstruct
  public void addMappings() {
    TypeMap<DiscoveryServiceSynchronizationJob, DiscoverySyncJobServiceDTO> typeMap =
        modelMapper.createTypeMap(
            DiscoveryServiceSynchronizationJob.class, DiscoverySyncJobServiceDTO.class);
  }
}
