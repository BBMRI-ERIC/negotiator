package eu.bbmri_eric.negotiator.post;

import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostModelMapper {

  private final ModelMapper modelMapper;
  private final PostTagResolver tagResolver;

  public PostModelMapper(ModelMapper modelMapper, PostTagResolver tagResolver) {
    this.modelMapper = modelMapper;
    this.tagResolver = tagResolver;
  }

  @PostConstruct
  public void addMappings() {
    TypeMap<Post, PostDTO> typeMap = modelMapper.createTypeMap(Post.class, PostDTO.class);

    typeMap.addMappings(mapping -> mapping.skip(PostDTO::setTags));

    typeMap.setPostConverter(
        context -> {
          PostDTO dto = context.getDestination();
          dto.setTags(tagResolver.resolve(context.getSource()));
          return dto;
        });
  }
}
