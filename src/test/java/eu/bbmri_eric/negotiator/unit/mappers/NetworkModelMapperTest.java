package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.network.NetworkDTO;
import eu.bbmri_eric.negotiator.governance.network.NetworkModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class NetworkModelMapperTest {

  @Spy public ModelMapper mapper = new ModelMapper();

  @InjectMocks NetworkModelMapper networkModelMapper;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.networkModelMapper.addMappings();
  }

  @Test
  void networkToDTO_map_Ok() {
    Network network =
        Network.builder()
            .externalId("test_network")
            .name("test")
            .uri("http://test.it")
            .contactEmail("test@test.it")
            .build();
    NetworkDTO networkDTO = mapper.map(network, NetworkDTO.class);
    assertEquals(network.getName(), networkDTO.getName());
    assertEquals(network.getExternalId(), networkDTO.getExternalId());
    assertEquals(network.getContactEmail(), networkDTO.getContactEmail());
    assertEquals(network.getUri(), networkDTO.getUri());
  }
}
