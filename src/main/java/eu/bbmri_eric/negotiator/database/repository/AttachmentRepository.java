package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.Attachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, String> {

  Optional<Attachment> findCompleteById(String lastname);

  Optional<Attachment> findMetadataById(String id);

  List<Attachment> getByNegotiationId(String negotiationId);

  Optional<Attachment> findMetadataByIdAndNegotiationId(String id, String negotiationId);
}
