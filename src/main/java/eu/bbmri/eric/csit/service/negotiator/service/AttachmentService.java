package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentMetadataDTO;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {

  //  void init();

  AttachmentMetadataDTO create(MultipartFile file);

  AttachmentMetadataDTO createForNegotiation(String negotiationId, MultipartFile file);

  AttachmentMetadataDTO findMetadataById(String id);

  AttachmentDTO findById(String id);
}
