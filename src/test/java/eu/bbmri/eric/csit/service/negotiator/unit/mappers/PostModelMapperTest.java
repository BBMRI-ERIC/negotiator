package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Post;
import eu.bbmri.eric.csit.service.negotiator.dto.post.PostDTO;
import eu.bbmri.eric.csit.service.negotiator.mappers.PostModelMapper;
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
