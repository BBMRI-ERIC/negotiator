package eu.bbmri.eric.csit.service.negotiator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.v3.TestUtils;
import eu.bbmri.eric.csit.service.negotiator.api.dto.query.QueryCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationRequest;
import eu.bbmri.eric.csit.service.negotiator.database.model.Query;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.repository.*;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class RequestServiceTest {

  @Autowired private RequestService requestService;
  @Autowired private QueryRepository queryRepository;
  @Autowired private RequestRepository requestRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private PersonRepository personRepository;
  @Mock
  private NegotiationRequestRepository negotiationRequestRepository;
  @Autowired private ModelMapper modelMapper;

  private Query createQueryEntity() {
    QueryCreateDTO queryRequest = TestUtils.createQueryRequest(false);
    Query query = modelMapper.map(queryRequest, Query.class);
    return queryRepository.save(query);
  }

  //  @Test
  public void testCreate_Ok_whenProjectIsIncluded() throws Exception {
    assert requestRepository.count() == 0;
    assert projectRepository.count() == 0;
    Query query = createQueryEntity();
    RequestCreateDTO request = TestUtils.createRequest(false, true, Set.of(query.getId()));
    Request requestEntity =
        requestService.create(request, 1L); // NB: 1L is the researcher Person id in test data

    assertEquals(requestRepository.count(), 1);
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
    NegotiationRequest negotiationRequest = new NegotiationRequest();
    negotiationRequest.setCreatorId("unique_id");
  }
}
