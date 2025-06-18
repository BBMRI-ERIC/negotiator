package eu.bbmri_eric.negotiator.attachment;

import eu.bbmri_eric.negotiator.attachment.dto.AttachmentDTO;
import eu.bbmri_eric.negotiator.attachment.dto.AttachmentMetadataDTO;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {

  AttachmentMetadataDTO create(MultipartFile file);

  /**
   * Upload an attachment for a negotiation.
   *
   * @param negotiationId The negotiation id.
   * @param organizationId The organization id.
   * @param file The file to upload.
   * @return The metadata of the uploaded attachment.
   */
  AttachmentMetadataDTO createForNegotiation(
      String negotiationId, @Nullable String organizationId, MultipartFile file);

  List<AttachmentMetadataDTO> findByNegotiation(String negotiationId);

  AttachmentMetadataDTO findByIdAndNegotiationId(String id, String negotiationId);

  /**
   * Delete an attachment
   * @param id of the attachment
   */
  void delete(String id);

  AttachmentDTO findById(String id);
}
