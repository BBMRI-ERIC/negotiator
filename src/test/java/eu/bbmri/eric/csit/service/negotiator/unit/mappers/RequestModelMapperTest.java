package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import eu.bbmri.eric.csit.service.negotiator.mappers.RequestModelsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class RequestModelMapperTest {
    @Spy
    public ModelMapper mapper = new ModelMapper();

    @InjectMocks
    RequestModelsMapper requestModelsMapper;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        this.requestModelsMapper.addMappings();
    }
}
