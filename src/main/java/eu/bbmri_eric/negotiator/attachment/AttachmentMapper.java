package eu.bbmri_eric.negotiator.attachment;

import eu.bbmri_eric.negotiator.attachment.dto.AttachmentDTO;
import eu.bbmri_eric.negotiator.attachment.dto.AttachmentMetadataDTO;
import jakarta.annotation.PostConstruct;
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

    TypeMap<MetadataAttachmentViewDTO, AttachmentMetadataDTO> projectionMapMetadata =
        modelMapper.createTypeMap(MetadataAttachmentViewDTO.class, AttachmentMetadataDTO.class);
  }
}
