package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static org.mockito.Mockito.when;

import eu.bbmri.eric.csit.service.negotiator.database.model.*;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PostRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.post.PostDTO;
import eu.bbmri.eric.csit.service.negotiator.integration.api.v3.TestUtils;
import eu.bbmri.eric.csit.service.negotiator.service.PostServiceImpl;
import eu.bbmri.eric.csit.service.negotiator.unit.context.WithMockNegotiatorUser;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class PostServiceTest {

  @Mock PostRepository postRepository;

  @Mock ModelMapper modelMapper;

  @InjectMocks PostServiceImpl postService;

  private static final long RESEARCHER_ID = 2L;
  private static final String RESEARCHER_AUTH_NAME = "researcher";
  private static final String RESEARCHER_AUTH_SUBJECT = "researcher@aai.eu";
  private static final String RESEARCHER_AUTH_EMAIL = "researcher@aai.eu";

  private static final long BIOBANKER_1_ID = 3L;
  private static final String BIOBANKER_1_AUTH_NAME = "biobanker_1";
  private static final String BIOBANKER_1_AUTH_SUBJECT = "biobanker_1@aai.eu";
  private static final String BIOBANKER_1_AUTH_EMAIL = "biobanker_1@aai.eu";

  private static final long BIOBANKER_2_ID = 4L;
  private static final String BIOBANKER_2_AUTH_NAME = "biobanker_2";
  private static final String BIOBANKER_2_AUTH_SUBJECT = "biobanker_2@aai.eu";
  private static final String BIOBANKER_2_AUTH_EMAIL = "biobanker_2@aai.eu";

  private static final long ADMIN_ID = 5L;
  private static final String ADMIN_AUTH_NAME = "admin";
  private static final String ADMIN_AUTH_SUBJECT = "admin@aai.eu";
  private static final String ADMIN_AUTH_EMAIL = "admin@aai.eu";

  private static final String ORG_1 = "Organization_1";
  private static final String ORG_2 = "Organization_2";

  private AutoCloseable closeable;

  private Post publicPost1;
  private Post publicPost2;
  private Post privateResToOrg1;
  private Post privateResToOrg2;
  private Post privateBio1ToOrg1;
  private Post privateBio2ToOrg2;

  @BeforeEach
  void before() {
    closeable = MockitoAnnotations.openMocks(this);

    Person researcher =
        Person.builder()
            .id(RESEARCHER_ID)
            .authName(RESEARCHER_AUTH_NAME)
            .authEmail(RESEARCHER_AUTH_EMAIL)
            .authSubject(RESEARCHER_AUTH_SUBJECT)
            .build();

    Person biobanker1 =
        Person.builder()
            .id(BIOBANKER_1_ID)
            .authName(BIOBANKER_1_AUTH_NAME)
            .authEmail(BIOBANKER_1_AUTH_EMAIL)
            .authSubject(BIOBANKER_1_AUTH_SUBJECT)
            .build();

    Person biobanker2 =
        Person.builder()
            .id(BIOBANKER_2_ID)
            .authName(BIOBANKER_2_AUTH_NAME)
            .authEmail(BIOBANKER_2_AUTH_EMAIL)
            .authSubject(BIOBANKER_2_AUTH_SUBJECT)
            .build();
    DataSource dataSource = new DataSource();

    Organization organization1 = Organization.builder().externalId(ORG_1).build();
    Organization organization2 = Organization.builder().externalId(ORG_2).build();

    Resource resource1 =
        Resource.builder()
            .dataSource(dataSource)
            .sourceId("resource:1")
            .name("Resource 1")
            .organization(organization1)
            .build();
    Resource resource2 =
        Resource.builder()
            .dataSource(dataSource)
            .sourceId("resource:2")
            .name("Resource 2")
            .organization(organization2)
            .build();
    organization1.setResources(Set.of(resource1));
    organization2.setResources(Set.of(resource2));

    Negotiation negotiation = Negotiation.builder().build();
    negotiation.setCreatedBy(researcher);

    publicPost1 =
        TestUtils.createPost(negotiation, researcher, null, "public post 1", PostType.PUBLIC);
    publicPost2 =
        TestUtils.createPost(negotiation, researcher, null, "public post 2", PostType.PUBLIC);

    privateResToOrg1 =
        TestUtils.createPost(
            negotiation, researcher, organization1, "private post from resercher to organization 1", PostType.PRIVATE);
    privateResToOrg2 =
        TestUtils.createPost(
            negotiation, researcher, organization2, "private post from resercher to organization 1", PostType.PRIVATE);

    privateBio1ToOrg1 =
        TestUtils.createPost(
            negotiation, biobanker1, organization1, "private post from biobanker 1 to organization 1", PostType.PRIVATE);
    privateBio2ToOrg2 =
        TestUtils.createPost(
            negotiation, biobanker2, organization2, "private post from biobanker 2 to organization 2", PostType.PRIVATE);
  }

  @AfterEach
  void after() throws Exception {
    closeable.close();
  }

  /** Tests that the admin gets all the posts of a negotiation */
  @Test
  @WithMockNegotiatorUser(
      id = 1L,
      authName = ADMIN_AUTH_NAME,
      authSubject = ADMIN_AUTH_SUBJECT,
      authEmail = ADMIN_AUTH_EMAIL,
      authorities = {"ROLE_ADMIN"})
  public void test_FindAllByNegotiationId_AsAdmin() {
    List<Post> posts =
        List.of(
            publicPost1,
            publicPost2,
            privateResToOrg1,
            privateResToOrg2,
            privateBio2ToOrg2,
            privateBio1ToOrg1);
    when(postRepository.findByNegotiationId("negotiationId")).thenReturn(posts);
    Assertions.assertEquals(
        posts.size(), postService.findByNegotiationId("negotiationId", null, null).size());
  }

  /** Tests that the researcger gets all the posts of a negotiation */
  @Test
  @WithMockNegotiatorUser(
      id = RESEARCHER_ID,
      authName = RESEARCHER_AUTH_NAME,
      authSubject = RESEARCHER_AUTH_SUBJECT,
      authEmail = RESEARCHER_AUTH_EMAIL,
      authorities = {"RESEARCHER"})
  public void test_FindAllPostByNegotiationId_AsResearcher() {
    List<Post> posts =
        List.of(
            publicPost1,
            publicPost2,
            privateResToOrg1,
            privateResToOrg2,
            privateBio2ToOrg2,
            privateBio1ToOrg1);
    when(postRepository.findByNegotiationId("negotiationId")).thenReturn(posts);
    Assertions.assertEquals(
        4, postService.findByNegotiationId("negotiationId", null, null).size());
  }

  /** Tests that the biobanker gets the public posts and the ones sent to their organization */
  @Test
  @WithMockNegotiatorUser(
      id = BIOBANKER_1_ID,
      authName = BIOBANKER_1_AUTH_NAME,
      authSubject = BIOBANKER_1_AUTH_SUBJECT,
      authEmail = BIOBANKER_1_AUTH_EMAIL,
      authorities = {"ROLE_REPRESENTATIVE_", "ROLE_REPRESENTATIVE_resource:1"})
  public void test_FindAllPostByNegotiationId_AsBiobanker() {
    List<Post> posts =
        List.of(
            publicPost1,
            publicPost2,
            privateResToOrg1,
            privateResToOrg2,
            privateBio2ToOrg2,
            privateBio1ToOrg1);

    when(postRepository.findByNegotiationId("negotiationId")).thenReturn(posts);
    Assertions.assertEquals(
        4, postService.findByNegotiationId("negotiationId", null, null).size());
  }

  //  @Test
  //  @WithMockUser
  //  public void test_FindPostByNegotiationId() {
  //    when(postRepository.findByNegotiationId("negotiationId"))
  //        .thenReturn(List.of(publicPost1, publicPost2));
  //    Assertions.assertEquals(2, postService.findByNegotiationId("negotiationId", null,
  // null).size());
  //  }
  //
  //  @Test
  //  public void test_FindPostByNegotiationIdNoResults() {
  //    when(postRepository.findByNegotiationId("fakeId")).thenReturn(Collections.emptyList());
  //    Assertions.assertEquals(0, postService.findByNegotiationId("fakeId", null, null).size());
  //  }
  //
  //  @Test
  //  public void test_FindPostByNegotiationIdAndPosters() {
  //    when(postRepository.findByNegotiationIdAndStatusAndCreatedBy_authNameIn(
  //            "negotiationId", PostStatus.CREATED, List.of("p1")))
  //        .thenReturn(List.of(publicPost1, publicPost2));
  //    Assertions.assertEquals(
  //        2,
  //        postService
  //            .findNewByNegotiationIdAndPosters("negotiationId", List.of("p1"), null, null)
  //            .size());
  //  }
  //
  //  @Test
  //  public void test_FindPostByNegotiationIdAndPosters_ReturnsEmptyList_whenNotFound() {
  //    when(postRepository.findByNegotiationIdAndStatusAndCreatedBy_authNameIn(
  //            any(), eq(PostStatus.CREATED), any()))
  //        .thenReturn(Collections.emptyList());
  //    assertTrue(
  //        postService
  //            .findNewByNegotiationIdAndPosters(
  //                "fakeID", Arrays.asList("fakep1", "fakeP2"), null, null)
  //            .isEmpty());
  //  }
  //
  //  @Test
  //  public void test_FindAllPrivatePosts() {
  //    when(postRepository.findByNegotiationIdAndType("negotiationId", PostType.PRIVATE))
  //        .thenReturn(List.of(privateResToOrg1, privateResToOrg2));
  //    Assertions.assertEquals(
  //        2, postService.findByNegotiationId("negotiationId", PostType.PRIVATE, null).size());
  //  }
  //
  //  @Test
  //  @WithMockUser(
  //      username = "admin",
  //      au = {"ADMIN"})
  //  public void test_FindAllPrivatePostsByOrganization() {
  //    when(postRepository.findByNegotiationIdAndTypeAndOrganization_ExternalId(
  //            "negotiationId", PostType.PRIVATE, "organization1"))
  //        .thenReturn(List.of(privateResToOrg1));
  //    Assertions.assertEquals(
  //        1, postService.findByNegotiationId("negotiationId", PostType.PRIVATE,
  // "resource1").size());
  //  }
}
