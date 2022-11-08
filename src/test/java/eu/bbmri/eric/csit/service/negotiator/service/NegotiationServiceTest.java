package eu.bbmri.eric.csit.service.negotiator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.v3.TestUtils;
import eu.bbmri.eric.csit.service.negotiator.api.dto.query.QueryCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiableEntity;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationRequest;
import eu.bbmri.eric.csit.service.negotiator.database.model.Query;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.repository.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class NegotiationServiceTest {

  @InjectMocks
  private NegotiationService negotiationService;
  @Autowired private RequestRepository requestRepository;
  @Autowired private NegotiationRepository negotiationRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private PersonRepository personRepository;
  @Mock
  private TempRequestRepository tempRequestRepository;
  @Autowired private ModelMapper modelMapper;

  private Query createQueryEntity() {
    QueryCreateDTO queryRequest = TestUtils.createQueryRequest(false);
    Query query = modelMapper.map(queryRequest, Query.class);
    return requestRepository.save(query);
  }

  //  @Test
  public void testCreate_Ok_whenProjectIsIncluded() throws Exception {
    assert negotiationRepository.count() == 0;
    assert projectRepository.count() == 0;
    Query query = createQueryEntity();
    RequestCreateDTO request = TestUtils.createRequest(false, true, Set.of(query.getId()));
    Request requestEntity =
        negotiationService.create(request, 1L); // NB: 1L is the researcher Person id in test data

    assertEquals(negotiationRepository.count(), 1);
    assertEquals(projectRepository.count(), 1);
    assertEquals(requestEntity.getTitle(), request.getTitle());
    assertEquals(requestEntity.getDescription(), request.getDescription());
    assertEquals(requestEntity.getProject().getTitle(), request.getProject().getTitle());
    assertEquals(
        requestEntity.getProject().getDescription(), request.getProject().getDescription());
    assertEquals(
        requestEntity.getProject().getExpectedDataGeneration(),
        request.getProject().getExpectedDataGeneration());
    assertEquals(
        requestEntity.getProject().getExpectedEndDate(), request.getProject().getExpectedEndDate());
    assertEquals(
        requestEntity.getProject().getIsTestProject(), request.getProject().getIsTestProject());
    assertEquals(requestEntity.getProject().getEthicsVote(), request.getProject().getEthicsVote());
  }

  @Test
  public void testCreateRequest(){
    NegotiationRequest negotiationRequest = getNegotiationRequest();
    negotiationService.createRequest(negotiationRequest);
    when(tempRequestRepository.findById(1L)).thenReturn(negotiationRequest);
    assertEquals(negotiationRequest, tempRequestRepository.findById(1L));
  }


  private static NegotiationRequest getNegotiationRequest() {
    NegotiationRequest negotiationRequest = new NegotiationRequest();
    negotiationRequest.setId(1L);
    negotiationRequest.setCreatorId("Man");
    negotiationRequest.setDataSourceId("Directory Test");
    Set<NegotiableEntity> negotiableEntities = new HashSet<>();
    negotiableEntities.add(NegotiableEntity.builder().id("idk").parentId("idk2").label("Test").build());
    negotiationRequest.setNegotiableEntities(negotiableEntities);
    return negotiationRequest;
  }

}
