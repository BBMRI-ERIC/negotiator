package eu.bbmri_eric.negotiator.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

/** Validates File type based on decoded mime type. */
@Component
@CommonsLog
public class FileTypeValidator implements Validator {

  private static final Tika tika = new Tika();
  private static final List<String> ALLOWED_MIME_TYPES =
      List.of(
          "application/pdf",
          "image/png",
          "image/jpeg",
          "application/msword", // .doc
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
          "text/plain", // .txt
          "text/csv", // .csv
          "application/vnd.ms-excel", // .xls (Excel 97-2003)
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
          );

  @Override
  public boolean supports(Class<?> clazz) {
    return MultipartFile.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    MultipartFile file = (MultipartFile) target;
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File cannot be empty");
    }

    try (InputStream inputStream = file.getInputStream()) {
      String detectedType = tika.detect(inputStream);
      log.debug("Detected attachment type: " + detectedType);
      if (!ALLOWED_MIME_TYPES.contains(detectedType)) {
        throw new UnsupportedFileTypeException(
            "File type "
                + detectedType
                + " is not supported. Try more conventional file formats such as PDF, JPEG or CSV");
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read file: " + file.getOriginalFilename());
    }
  }
}
