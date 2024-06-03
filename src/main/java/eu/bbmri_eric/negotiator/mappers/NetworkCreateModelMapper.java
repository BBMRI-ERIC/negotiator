package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.database.model.Network;
import eu.bbmri_eric.negotiator.dto.network.NetworkCreateDTO;
import org.springframework.stereotype.Component;

@Component
public class NetworkCreateModelMapper {

  public Network mapToEntity(NetworkCreateDTO networkCreateDTO) {
    return Network.builder()
        .externalId(networkCreateDTO.getExternalId())
        .name(networkCreateDTO.getName())
        .contactEmail(networkCreateDTO.getContactEmail())
        .uri(networkCreateDTO.getUri())
        .build();
  }

  public NetworkCreateDTO mapToDTO(Network network) {
    return NetworkCreateDTO.builder()
        .externalId(network.getExternalId())
        .name(network.getName())
        .contactEmail(network.getContactEmail())
        .uri(network.getUri())
        .build();
  }
}
