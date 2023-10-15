package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.database.model.Attachment;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentMetadataDTO;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class AttachmentMapper {
  @Autowired ModelMapper modelMapper;

  @PostConstruct
  public void addMappings() {
    TypeMap<Attachment, AttachmentDTO> typeMap =
        modelMapper.createTypeMap(Attachment.class, AttachmentDTO.class);

    TypeMap<Attachment, AttachmentMetadataDTO> typeMapMetadata =
        modelMapper.createTypeMap(Attachment.class, AttachmentMetadataDTO.class);
  }
}
