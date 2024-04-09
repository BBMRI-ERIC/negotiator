package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.attachments.Attachment;
import eu.bbmri_eric.negotiator.database.model.attachments.AttachmentProjection;
import eu.bbmri_eric.negotiator.database.model.attachments.MetadataAttachmentProjection;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentMetadataDTO;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.modelmapper.Converter;
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

    TypeMap<AttachmentProjection, AttachmentDTO> projectionMap =
        modelMapper.createTypeMap(AttachmentProjection.class, AttachmentDTO.class);

    TypeMap<MetadataAttachmentProjection, AttachmentMetadataDTO> projectionMapMetadata =
        modelMapper.createTypeMap(MetadataAttachmentProjection.class, AttachmentMetadataDTO.class);

    Converter<Organization, String> organizationConverter =
        o -> {
          if (o.getSource() != null) {
            return o.getSource().getExternalId();
          } else {
            return null;
          }
        };

    projectionMapMetadata.addMappings(
        mapper ->
            mapper
                .using(organizationConverter)
                .map(
                    MetadataAttachmentProjection::getOrganization,
                    AttachmentMetadataDTO::setOrganizationId));
  }
}
