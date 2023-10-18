package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri.eric.csit.service.negotiator.database.model.*;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PostRepository;
import eu.bbmri.eric.csit.service.negotiator.integration.api.v3.TestUtils;
import eu.bbmri.eric.csit.service.negotiator.service.PostServiceImpl;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;

public class PostServiceTest {

  @Mock PostRepository postRepository;

  @Mock ModelMapper modelMapper;

  @InjectMocks PostServiceImpl postService;

  private AutoCloseable closeable;

  private Post publicPost1;
  private Post publicPost2;
  private Post privatePost1;
  private Post privatePost2;

  @BeforeEach
  void before() throws IOException {
    closeable = MockitoAnnotations.openMocks(this);
    Negotiation negotiation = new Negotiation();
    Organization organization1 = Organization.builder().externalId("organization1").build();
    Organization organization2 = Organization.builder().externalId("organization2").build();

    publicPost1 = TestUtils.createPost(negotiation, null, "public post 1", PostType.PUBLIC);
    publicPost2 = TestUtils.createPost(negotiation, null, "public post 2", PostType.PUBLIC);
    privatePost1 =
        TestUtils.createPost(negotiation, organization1, "private post 1", PostType.PRIVATE);
    privatePost2 =
        TestUtils.createPost(negotiation, organization2, "private post 2", PostType.PRIVATE);
  }

  @AfterEach
  void after() throws Exception {
    closeable.close();
  }

  @Test
  public void test_FindPostByNegotiationId() {
    when(postRepository.findByNegotiationId("negotiationId"))
        .thenReturn(List.of(publicPost1, publicPost2));
    Assertions.assertEquals(2, postService.findByNegotiationId("negotiationId", null, null).size());
  }

  @Test
  public void test_FindPostByNegotiationIdNoResults() {
    when(postRepository.findByNegotiationId("fakeId")).thenReturn(Collections.emptyList());
    Assertions.assertEquals(0, postService.findByNegotiationId("fakeId", null, null).size());
  }

  @Test
  public void test_FindPostByNegotiationIdAndPosters() {
    when(postRepository.findNewByNegotiationIdAndPosters("negotiationId", List.of("p1")))
        .thenReturn(List.of(publicPost1, publicPost2));
    Assertions.assertEquals(
        2,
        postService
            .findNewByNegotiationIdAndPosters("negotiationId", List.of("p1"), null, null)
            .size());
  }

  @Test
  public void test_FindPostByNegotiationIdAndPosters_ReturnsEmptyList_whenNotFound() {
    when(postRepository.findNewByNegotiationIdAndPosters(any(), any()))
        .thenReturn(Collections.emptyList());
    assertTrue(
        postService
            .findNewByNegotiationIdAndPosters(
                "fakeID", Arrays.asList("fakep1", "fakeP2"), null, null)
            .isEmpty());
  }

  @Test
  public void test_FindAllPrivatePosts() {
    when(postRepository.findByNegotiationIdAndType("negotiationId", PostType.PRIVATE))
        .thenReturn(List.of(privatePost1, privatePost2));
    Assertions.assertEquals(
        2, postService.findByNegotiationId("negotiationId", PostType.PRIVATE, null).size());
  }

  @Test
  public void test_FindAllPrivatePostsByOrganization() {
    when(postRepository.findByNegotiationIdAndTypeAndOrganization(
            "negotiationId", PostType.PRIVATE, "organization1"))
        .thenReturn(List.of(privatePost1));
    Assertions.assertEquals(
        1,
        postService.findByNegotiationId("negotiationId", PostType.PRIVATE, "organization1").size());
  }
}
