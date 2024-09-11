package eu.bbmri_eric.negotiator.governance.network;

import jakarta.annotation.PostConstruct;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
@CommonsLog
public class NetworkModelMapper {

  @Autowired ModelMapper modelMapper;

  public NetworkModelMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  @PostConstruct
  public void addMappings() {
    TypeMap<Network, NetworkDTO> typeMap =
        modelMapper.createTypeMap(Network.class, NetworkDTO.class);
  }
}
