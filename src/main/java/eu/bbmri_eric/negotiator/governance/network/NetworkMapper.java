package eu.bbmri_eric.negotiator.governance.network;

import java.util.List;
import java.util.stream.Collectors;

public class NetworkMapper {
  public static NetworkDTO toDto(Network network) {
    NetworkDTO orgDTO =
        NetworkDTO.builder()
            .id(network.getId())
            .name(network.getName())
            .externalId(network.getExternalId())
            .uri(network.getUri())
            .contactEmail(network.getContactEmail())
            .build();
    return orgDTO;
  }

  public static List<NetworkDTO> toDtoList(List<Network> networks) {
    return networks.stream().map(NetworkMapper::toDto).collect(Collectors.toList());
  }
}
