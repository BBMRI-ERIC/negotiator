package eu.bbmri.eric.csit.service.negotiator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.v3.TestUtils;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ProjectRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RequestRepository;
import java.util.Set;
import org.mockito.InjectMocks;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class tempDataSourceTest {

  @InjectMocks
  private NegotiationService negotiationService;
  @Autowired
  private RequestRepository requestRepository;
  @Autowired
  private NegotiationRepository negotiationRepository;
  @Autowired
  private ProjectRepository projectRepository;
  @Autowired
  private PersonRepository personRepository;
  @Autowired
  private ModelMapper modelMapper;

  private Request createQueryEntity() {
    RequestCreateDTO queryRequest = TestUtils.createRequest(false);
    Request request = modelMapper.map(queryRequest, Request.class);
    return requestRepository.save(request);
  }

  //  @Test
  public void testCreate_Ok_whenProjectIsIncluded() throws Exception {
    assert negotiationRepository.count() == 0;
    assert projectRepository.count() == 0;
    Request query = createQueryEntity();
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true, Set.of(query.getId()));
    Negotiation negotiationEntity =
        negotiationService.create(request, 1L); // NB: 1L is the researcher Person id in test data

    assertEquals(negotiationRepository.count(), 1);
    assertEquals(projectRepository.count(), 1);
    assertEquals(negotiationEntity.getTitle(), request.getTitle());
    assertEquals(negotiationEntity.getDescription(), request.getDescription());
//    assertEquals(negotiationEntity.getProject().getTitle(), request.getProject().getTitle());
//    assertEquals(
//        negotiationEntity.getProject().getDescription(), request.getProject().getDescription());
//    assertEquals(
//        negotiationEntity.getProject().getExpectedDataGeneration(),
//        request.getProject().getExpectedDataGeneration());
//    assertEquals(
//        negotiationEntity.getProject().getExpectedEndDate(),
//        request.getProject().getExpectedEndDate());
//    assertEquals(
//        negotiationEntity.getProject().getIsTestProject(), request.getProject().getIsTestProject());
//    assertEquals(negotiationEntity.getProject().getEthicsVote(),
//        request.getProject().getEthicsVote());
  }


}
