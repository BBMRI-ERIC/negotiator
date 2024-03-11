package eu.bbmri_eric.negotiator.unit.mappers;

import eu.bbmri_eric.negotiator.mappers.AccessFormModelsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;

public class AccessFormElementMapperTest {
  public ModelMapper mapper = new ModelMapper();
  public AccessFormModelsMapper accessFormModelsMapper = new AccessFormModelsMapper(mapper);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.accessFormModelsMapper.addMappings();
  }

  @Test
  void name() {}
}
