package eu.bbmri_eric.negotiator.info_submission;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InformationSubmissionSummaryDTO {
  private String filename;
  private byte[] content;
  private String contentType;
}
