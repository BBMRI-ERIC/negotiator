package eu.bbmri_eric.negotiator.attachment;

import eu.bbmri_eric.negotiator.attachment.dto.AttachmentDTO;
import eu.bbmri_eric.negotiator.attachment.dto.AttachmentMetadataDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v3")
@Tag(name = "Attachments", description = "Upload and download attachments")
@SecurityRequirement(name = "security_auth")
public class AttachmentController {

  private final AttachmentService storageService;
  private final FileTypeValidator fileTypeValidator;

  public AttachmentController(
      AttachmentService storageService, FileTypeValidator fileTypeValidator) {
    this.storageService = storageService;
    this.fileTypeValidator = fileTypeValidator;
  }

  @InitBinder
  protected void initBinder(WebDataBinder binder) {
    binder.addValidators(fileTypeValidator);
  }

  @PostMapping(
      value = "/negotiations/{negotiationId}/attachments",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Upload an attachment to a specific Negotiation")
  @ResponseStatus(HttpStatus.CREATED)
  public AttachmentMetadataDTO createForNegotiation(
      @PathVariable @Schema(example = "negotiation-1") String negotiationId,
      @RequestParam("file") @Valid @Schema(description = "File to be uploaded") MultipartFile file,
      @Nullable
          @RequestParam("organizationId")
          @Schema(
              example = "biobank:1",
              description = "External ID of the Organization the attachment is for")
          String organizationId) {
    fileTypeValidator.validateObject(file);
    return storageService.createForNegotiation(negotiationId, organizationId, file);
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
    return storageService.findByIdAndNegotiationId(attachmentId, negotiationId);
  }

  @PostMapping(
      value = "/attachments",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Post an attachment",
      description = "Upload an attachment that you can then reference in a Negotiation")
  @ResponseStatus(HttpStatus.CREATED)
  public AttachmentMetadataDTO create(
      @RequestParam("file") @Valid @Schema(description = "File to be uploaded")
          MultipartFile file) {
    fileTypeValidator.validateObject(file);
    return storageService.create(file);
  }

  @GetMapping(value = "/attachments/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Retrieve an attachment")
  public ResponseEntity<byte[]> retrieve(@PathVariable String id) {
    AttachmentDTO attachmentInfo = storageService.findById(id);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            String.format("attachment; filename=\"%s\"", attachmentInfo.getName()))
        .contentType(MediaType.valueOf(attachmentInfo.getContentType()))
        .body(attachmentInfo.getPayload());
  }

  @DeleteMapping(value = "/attachments/{id}")
  @Operation(summary = "Delete an attachment")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    storageService.deleteById(id);
  }
}
