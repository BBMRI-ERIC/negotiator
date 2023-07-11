package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.mappers.RequestModelsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    @Test
    void testBasicMapping() {
        Request request = new Request();
        request.setId("newRequest");
        Resource resource = new Resource();
        resource.setSourceId("collection:1");
        request.setResources(Set.of(resource));
        assertEquals(request.getId(), this.mapper.map(request, RequestDTO.class).getId());
    }
}
