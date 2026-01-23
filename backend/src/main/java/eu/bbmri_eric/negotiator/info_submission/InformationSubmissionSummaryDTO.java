package eu.bbmri_eric.negotiator.info_submission;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InformationSubmissionSummaryDTO implements MultipartFile {
  private String filename;
  private byte[] content;
  private String contentType;

  @Override
  public String getName() {
    return filename;
  }

  @Override
  public @Nullable String getOriginalFilename() {
    return filename;
  }

  @Override
  public boolean isEmpty() {
    return content.length == 0;
  }

  @Override
  public long getSize() {
    return content.length;
  }

  @Override
  public byte[] getBytes() throws IOException {
    return content;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(content);
  }

  @Override
  public void transferTo(File dest) throws IOException, IllegalStateException {
    Files.write(dest.toPath(), content);
  }
}
