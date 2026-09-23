package eu.bbmri_eric.negotiator.attachment;

import eu.bbmri_eric.negotiator.attachment.dto.AttachmentDTO;
import eu.bbmri_eric.negotiator.attachment.dto.AttachmentMetadataDTO;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;

/** Service for converting attachments to PDF format. */
@Service(value = "DefaultAttachmentConversionService")
@CommonsLog
public class AttachmentConversionServiceImpl implements AttachmentConversionService {
  private static final String CONTENT_TYPE_PDF = "application/pdf";
  private static final String CONTENT_TYPE_DOCX =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final String CONTENT_TYPE_TIKA_OOXML = "application/x-tika-ooxml";
  private static final String CONTENT_TYPE_DOC = "application/msword";
  private static final String CONTENT_TYPE_TIKA_MSOFFICE = "application/x-tika-msoffice";
  private static final String CONTENT_TYPE_TIKA_XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final AttachmentService attachmentService;

  public AttachmentConversionServiceImpl(AttachmentService attachmentService) {
    this.attachmentService = attachmentService;
  }

  public List<byte[]> listByNegotiationIdToPdf(String negotiationId) {
    List<String> attachmentIds =
        attachmentService.findByNegotiation(negotiationId).stream()
            .map(AttachmentMetadataDTO::getId)
            .toList();

    if (attachmentIds.isEmpty()) {
      log.warn("No valid attachments found for negotiation");
      return List.of();
    }

    return listToPdf(attachmentIds);
  }

  public List<byte[]> listToPdf(List<String> attachmentIds) {
    if (attachmentIds == null || attachmentIds.isEmpty()) {
      throw new IllegalArgumentException("Attachment IDs list cannot be null or empty");
    }

    log.debug("Converting attachments to PDF: " + attachmentIds.size());

    List<AttachmentDTO> attachmentsList =
        attachmentIds.stream()
            .filter(Objects::nonNull)
            .map(
                id -> {
                  try {
                    return attachmentService.findById(id);
                  } catch (Exception e) {
                    log.error("Failed to retrieve attachment with ID: " + id, e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (attachmentsList.isEmpty()) {
      log.warn("No valid attachments found for conversion");
      return List.of();
    }

    return convertAttachmentsToPdf(attachmentsList);
  }

  private List<byte[]> convertAttachmentsToPdf(List<AttachmentDTO> attachmentsList) {
    return attachmentsList.stream()
        .map(this::convertSingleAttachmentToPdf)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private byte[] convertSingleAttachmentToPdf(AttachmentDTO attachmentDTO) {
    if (attachmentDTO == null) {
      log.warn("Attachment DTO is null, skipping conversion");
      return null;
    }

    try {
      String contentType = attachmentDTO.getContentType();
      byte[] payload = attachmentDTO.getPayload();

      if (contentType == null) {
        log.error("Content type is null for attachment");
        return null;
      }

      if (payload == null || payload.length == 0) {
        log.error("Payload is null or empty for attachment with content type: " + contentType);
        return null;
      }

      log.debug("Converting attachment with content type: " + contentType);
      try {
        FileTypeConverter converter = getConverter(contentType);
        return converter.convertToPdf(payload);
      } catch (IllegalArgumentException ex) {
        return null;
      }
    } catch (Exception e) {
      log.error("Error converting attachment to PDF: " + e.getMessage());
      return null;
    }
  }

  private FileTypeConverter getConverter(String contentType) {
    return switch (contentType) {
      case CONTENT_TYPE_PDF -> {
        log.debug("Attachment is already PDF, returning as-is");
        yield new PDFConverter();
      }
      case CONTENT_TYPE_DOCX, CONTENT_TYPE_TIKA_OOXML -> new DocxConverter();
      case CONTENT_TYPE_DOC, CONTENT_TYPE_TIKA_MSOFFICE -> new DocConverter();
      case CONTENT_TYPE_TIKA_XLSX -> new XlsxConverter();
      default -> {
        throw new IllegalArgumentException("Unsupported content type");
      }
    };
  }
}
