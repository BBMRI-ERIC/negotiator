package eu.bbmri.eric.csit.service.negotiator.mappers;

import eu.bbmri.eric.csit.service.negotiator.database.model.Post;
import eu.bbmri.eric.csit.service.negotiator.dto.post.PostDTO;
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
