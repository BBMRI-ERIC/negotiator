package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.post.PostDTO;
import eu.bbmri_eric.negotiator.post.PostModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class PostModelMapperTest {
  @Spy public ModelMapper mapper = new ModelMapper();

  @InjectMocks PostModelMapper postModelMapper;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.postModelMapper.addMappings();
  }

  @Test
  public void map_PostToDTO_ok() {
    Post post = Post.builder().negotiation(new Negotiation()).text("This is important").build();
    PostDTO postDTO = mapper.map(post, PostDTO.class);
    assertEquals(post.getText(), postDTO.getText());
  }
}
