package eu.bbmri_eric.negotiator.attachment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, String> {
  @Query(
      "SELECT new eu.bbmri_eric.negotiator.database.model.views.MetadataAttachmentViewDTO("
          + "a.id, a.name, a.size, a.contentType, o.id, o.externalId, a.negotiation.id, a.createdBy.id) "
          + "FROM Attachment a "
          + "LEFT JOIN a.organization o "
          + "WHERE a.negotiation.id = :negotiationId")
  List<MetadataAttachmentViewDTO> findByNegotiationId(String negotiationId);

  @Query(
      "SELECT new eu.bbmri_eric.negotiator.database.model.views.AttachmentViewDTO("
          + "a.id, a.name, a.size, a.contentType, a.payload, o.id, o.externalId, a.negotiation.id, a.createdBy.id) "
          + "FROM Attachment a "
          + "LEFT JOIN a.organization o "
          + "WHERE a.id = :attachmentId")
  Optional<AttachmentViewDTO> findAllById(String attachmentId);

  @Query(
      "SELECT new eu.bbmri_eric.negotiator.database.model.views.MetadataAttachmentViewDTO("
          + "a.id, a.name, a.size, a.contentType, o.id, o.externalId, a.negotiation.id, a.createdBy.id) "
          + "FROM Attachment a "
          + "LEFT JOIN a.organization o "
          + "WHERE a.id = :attachmentId and a.negotiation.id = :negotiationId")
  Optional<MetadataAttachmentViewDTO> findMetadataByIdAndNegotiationId(
      String attachmentId, String negotiationId);

  @Query(
      "SELECT new eu.bbmri_eric.negotiator.database.model.views.MetadataAttachmentViewDTO("
          + "a.id, a.name, a.size, a.contentType, o.id, o.externalId, a.negotiation.id, a.createdBy.id) "
          + "FROM Attachment a "
          + "LEFT JOIN a.organization o "
          + "WHERE a.id = :attachmentId")
  Optional<MetadataAttachmentViewDTO> findMetadataById(String attachmentId);
}
