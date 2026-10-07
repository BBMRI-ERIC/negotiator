package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostDTO;
import eu.bbmri_eric.negotiator.post.PostType;
import eu.bbmri_eric.negotiator.user.Person;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class PostModelMapperTest {
  @Spy public ModelMapper mapper = new ModelMapper();

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void map_PostToDTO_ok() {
    Post post =
        Post.builder()
            .id("test-id")
            .type(PostType.PUBLIC)
            .negotiation(Negotiation.builder().id("test-negotiation-id").build())
            .text("This is important")
            .build();
    post.setCreationDate(LocalDateTime.now());
    post.setCreatedBy(new Person());
    PostDTO postDTO = mapper.map(post, PostDTO.class);
    assertEquals(post.getText(), postDTO.getText());
    assertEquals(post.getNegotiation().getId(), postDTO.getNegotiationId());
  }

  @Test
  public void map_PostToDTO_keepsIdAndReplyToIdApart() {
    Post post = buildPost();
    post.setReplyToId("original-id");
    PostDTO postDTO = mapper.map(post, PostDTO.class);
    assertEquals("test-id", postDTO.getId());
    assertEquals("original-id", postDTO.getReplyToId());
  }

  @Test
  public void map_PostToDTO_withoutReplyToId() {
    PostDTO postDTO = mapper.map(buildPost(), PostDTO.class);
    assertEquals("test-id", postDTO.getId());
    assertNull(postDTO.getReplyToId());
  }

  private static Post buildPost() {
    Post post =
        Post.builder()
            .id("test-id")
            .type(PostType.PUBLIC)
            .negotiation(Negotiation.builder().id("test-negotiation-id").build())
            .text("This is important")
            .build();
    post.setCreationDate(LocalDateTime.now());
    post.setCreatedBy(new Person());
    return post;
  }
}
