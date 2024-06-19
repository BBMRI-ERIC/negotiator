package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.database.model.Network;
import eu.bbmri_eric.negotiator.dto.network.NetworkCreateDTO;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NetworkCreateModelMapper {

  @Autowired ModelMapper modelMapper;

  @PostConstruct
  public void addMappings() {
    TypeMap<NetworkCreateDTO, Network> typeMap =
        modelMapper.getTypeMap(NetworkCreateDTO.class, Network.class);
    if (typeMap == null) { // Check if TypeMap already exists
      typeMap = modelMapper.createTypeMap(NetworkCreateDTO.class, Network.class);
    }
    typeMap.addMappings(
        mapper -> mapper.map(NetworkCreateDTO::getExternalId, Network::setExternalId));
    typeMap.addMappings(mapper -> mapper.map(NetworkCreateDTO::getName, Network::setName));
    typeMap.addMappings(mapper -> mapper.map(NetworkCreateDTO::getUri, Network::setUri));
    typeMap.addMappings(
        mapper -> mapper.map(NetworkCreateDTO::getContactEmail, Network::setContactEmail));

    typeMap.addMappings(mapper -> mapper.skip(Network::setId));
  }
}
