package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentMetadataDTO;
import eu.bbmri_eric.negotiator.service.AttachmentService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v3")
public class AttachmentController {

  private final AttachmentService storageService;

  public AttachmentController(AttachmentService storageService) {
    this.storageService = storageService;
  }

  @PostMapping(
      value = "/negotiations/{negotiationId}/attachments",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public AttachmentMetadataDTO createForNegotiation(
      @PathVariable String negotiationId,
      @RequestParam("file") MultipartFile file,
      @Nullable @RequestParam("organizationId") String organizationId) {
    return storageService.createForNegotiation(
        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId(),
        negotiationId,
        organizationId,
        file);
  }

  @GetMapping(
      value = "/negotiations/{negotiationId}/attachments",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public List<AttachmentMetadataDTO> listByNegotiation(@PathVariable String negotiationId) {
    return storageService.findByNegotiation(negotiationId);
  }

  @GetMapping(
      value = "/negotiations/{negotiationId}/attachments/{attachmentId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public AttachmentMetadataDTO getByNegotiationAndId(
      @PathVariable String negotiationId, @PathVariable String attachmentId) {
    return storageService.findByIdAndNegotiation(attachmentId, negotiationId);
  }

  @PostMapping(
      value = "/attachments",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public AttachmentMetadataDTO create(@RequestParam("file") MultipartFile file) {
    return storageService.create(file);
  }

  @GetMapping(value = "/attachments/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(HttpStatus.OK)
  //  @ResponseBody
  public ResponseEntity<byte[]> retrieve(@PathVariable String id) {
    AttachmentDTO attachmentInfo = storageService.findById(id);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            String.format("attachment; filename=\"%s\"", attachmentInfo.getName()))
        .contentType(MediaType.valueOf(attachmentInfo.getContentType()))
        .body(attachmentInfo.getPayload());
  }
}
