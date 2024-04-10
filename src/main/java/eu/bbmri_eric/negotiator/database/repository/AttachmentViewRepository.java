package eu.bbmri_eric.negotiator.database.repository;

import com.blazebit.persistence.spring.data.repository.EntityViewRepository;
import eu.bbmri_eric.negotiator.database.model.views.AttachmentView;
import eu.bbmri_eric.negotiator.database.model.views.MetadataAttachmentView;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

// @Repository
@Transactional(readOnly = true)
public interface AttachmentViewRepository
    extends EntityViewRepository<MetadataAttachmentView, String> {

  List<MetadataAttachmentView> findByNegotiationId(String negotiationId);

  Optional<AttachmentView> findById(String lastname);

  Optional<MetadataAttachmentView> findMetadataById(String id);

  List<MetadataAttachmentView> getByNegotiationId(String negotiationId);

  Optional<MetadataAttachmentView> findMetadataByIdAndNegotiationId(
      String id, String negotiationId);
}
