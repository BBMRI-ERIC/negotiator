package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Attachment;
import eu.bbmri.eric.csit.service.negotiator.database.repository.AttachmentRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentMetadataDTO;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service(value = "DefaultAttachmentService")
public class DBAttachmentService implements AttachmentService {

  @Autowired private final AttachmentRepository attachmentRepository;
  private final ModelMapper modelMapper;

  @Autowired
  public DBAttachmentService(AttachmentRepository attachmentRepository, ModelMapper modelMapper) {
    this.attachmentRepository = attachmentRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  public AttachmentMetadataDTO create(MultipartFile file) {
    Attachment attachment;
    try {
      attachment =
          Attachment.builder()
              .name(file.getOriginalFilename())
              .payload(file.getBytes())
              .contentType(file.getContentType())
              .size(file.getSize())
              .build();
      Attachment saved = attachmentRepository.save(attachment);
      return modelMapper.map(saved, AttachmentMetadataDTO.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public AttachmentMetadataDTO findMetadataById(String id) {
    Attachment attachment =
        attachmentRepository.findMetadataById(id).orElseThrow(EntityNotFoundException::new);
    return modelMapper.map(attachment, AttachmentMetadataDTO.class);
  }

  @Override
  public AttachmentDTO findById(String id) {
    Attachment attachment =
        attachmentRepository.findById(id).orElseThrow(EntityNotFoundException::new);
    return modelMapper.map(attachment, AttachmentDTO.class);
  }

  @Override
  public List<AttachmentMetadataDTO> getAllAttachments() {
    return attachmentRepository.findAll().stream()
        .map((element) -> modelMapper.map(element, AttachmentMetadataDTO.class))
        .collect(Collectors.toList());
  }
}
