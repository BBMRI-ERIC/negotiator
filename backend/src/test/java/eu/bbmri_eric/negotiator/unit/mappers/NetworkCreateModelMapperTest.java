package eu.bbmri_eric.negotiator.unit.mappers;


import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.network.NetworkCreateDTO;
import eu.bbmri_eric.negotiator.governance.network.NetworkCreateModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class NetworkCreateModelMapperTest {

  @Spy public ModelMapper mapper = new ModelMapper();

  @InjectMocks NetworkCreateModelMapper networkCreateModelMapper;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.networkCreateModelMapper.addMappings();
  }

  @Test
  void createDtoToNetwork_map_ok() {
    NetworkCreateDTO ntwCreateDto =
        NetworkCreateDTO.builder()
            .name("network")
            .description("network desc")
            .externalId("network1")
            .contactEmail("network@test.org")
            .uri("http://network.org")
            .build();
    Network network = mapper.map(ntwCreateDto, Network.class);
    assertEquals(network.getName(), ntwCreateDto.getName());
    assertEquals(network.getDescription(), ntwCreateDto.getDescription());
    assertEquals(network.getExternalId(), ntwCreateDto.getExternalId());
    assertEquals(network.getContactEmail(), ntwCreateDto.getContactEmail());
    assertEquals(network.getUri(), ntwCreateDto.getUri());
  }
}
