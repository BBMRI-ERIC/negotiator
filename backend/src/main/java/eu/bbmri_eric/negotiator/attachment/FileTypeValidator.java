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

/** Validates file type based on decoded MIME type and file extension. */
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

  private static final List<String> ALLOWED_EXTENSIONS =
      List.of("pdf", "png", "jpeg", "jpg", "doc", "docx", "txt", "csv", "xls", "xlsx");

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
    String originalFilename = file.getOriginalFilename();
    validateFileExtension(originalFilename);

    try (InputStream inputStream = file.getInputStream()) {
      validateMimeType(inputStream);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read file: " + originalFilename, e);
    }
  }

  /**
   * Validates the file extension against a whitelist.
   *
   * @param originalFilename the original file name
   */
  private void validateFileExtension(String originalFilename) {
    String fileExtension = getFileExtension(originalFilename);
    log.debug("Detected file extension: " + fileExtension);
    if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
      throw new UnsupportedFileTypeException(
          "File extension '"
              + fileExtension
              + "' is not supported. Allowed extensions are: "
              + ALLOWED_EXTENSIONS);
    }
  }

  /**
   * Validates the MIME type by analyzing the file's input stream.
   *
   * @param inputStream the input stream of the file
   * @throws IOException if an I/O error occurs reading the stream
   */
  private void validateMimeType(InputStream inputStream) throws IOException {
    String detectedType = tika.detect(inputStream);
    log.debug("Detected MIME type: " + detectedType);
    if (!ALLOWED_MIME_TYPES.contains(detectedType)) {
      throw new UnsupportedFileTypeException(
          "File type '"
              + detectedType
              + "' is not supported. Try more conventional file formats such as PDF, JPEG or CSV");
    }
  }

  /**
   * Extracts the file extension from the given file name.
   *
   * @param fileName the original file name
   * @return the file extension in lower case, or an empty string if none is found
   */
  private String getFileExtension(String fileName) {
    if (fileName == null || !fileName.contains(".")) {
      return "";
    }
    return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
  }
}
