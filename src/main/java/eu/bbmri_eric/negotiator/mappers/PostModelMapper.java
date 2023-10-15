package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.database.model.Post;
import eu.bbmri_eric.negotiator.dto.post.PostDTO;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostModelMapper {
  @Autowired ModelMapper modelMapper;

  @PostConstruct
  public void addMappings() {
    TypeMap<Post, PostDTO> typeMap = modelMapper.createTypeMap(Post.class, PostDTO.class);
  }
}
