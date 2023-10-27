package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentMetadataDTO;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {

  AttachmentMetadataDTO create(MultipartFile file);

  AttachmentMetadataDTO createForNegotiation(
      String negotiationId, @Nullable String organizationId, MultipartFile file);

  AttachmentMetadataDTO findMetadataById(String id);

  List<AttachmentMetadataDTO> findByNegotiation(String negotiationId);

  AttachmentMetadataDTO findByIdAndNegotiation(String id, String negotiationId);

  AttachmentDTO findById(String id);
}
