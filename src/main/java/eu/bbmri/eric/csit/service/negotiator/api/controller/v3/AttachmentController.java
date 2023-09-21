package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentMetadataDTO;
import eu.bbmri.eric.csit.service.negotiator.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v3")
public class AttachmentController {

  private final AttachmentService storageService;

  @Autowired
  public AttachmentController(AttachmentService storageService) {
    this.storageService = storageService;
  }

  @PostMapping(
      value = "/attachments",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public AttachmentMetadataDTO create(@RequestParam("file") MultipartFile file) {
    return storageService.create(file);
  }

  @PostMapping(
      value = "/negotiations/{negotiationId}/attachments",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public AttachmentMetadataDTO createForNegotiation(
      @PathVariable String negotiationId, @RequestParam("file") MultipartFile file) {
    return storageService.createForNegotiation(negotiationId, file);
  }

  @GetMapping(value = "/attachments/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @ResponseBody
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
