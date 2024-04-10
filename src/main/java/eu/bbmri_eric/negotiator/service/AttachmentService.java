package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentMetadataDTO;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {

  AttachmentMetadataDTO create(MultipartFile file);

  /**
   * Upload an attachment for a negotiation.
   *
   * @param userId The user id of the person uploading the attachment.
   * @param negotiationId The negotiation id.
   * @param organizationId The organization id.
   * @param file The file to upload.
   * @return The metadata of the uploaded attachment.
   */
  AttachmentMetadataDTO createForNegotiation(
      Long userId, String negotiationId, @Nullable String organizationId, MultipartFile file);

  List<AttachmentMetadataDTO> findByNegotiation(String negotiationId);

  AttachmentMetadataDTO findByIdAndNegotiationId(String id, String negotiationId);

  AttachmentDTO findById(String id);
}
