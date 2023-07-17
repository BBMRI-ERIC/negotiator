package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Post;
import eu.bbmri.eric.csit.service.negotiator.dto.post.PostDTO;
import eu.bbmri.eric.csit.service.negotiator.mappers.PostModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostModelMapperTest {
    @Spy
    public ModelMapper mapper = new ModelMapper();

    @InjectMocks
    PostModelMapper postModelMapper;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        this.postModelMapper.addMappings();
    }
    @Test
    public void map_PostToDTO_ok() {
    Post post = Post.builder().negotiation(new Negotiation())
            .poster(new Person())
            .text("This is important")
            .build();
    PostDTO postDTO = mapper.map(post, PostDTO.class);
    assertEquals(post.getText(), postDTO.getText());
    }
}
