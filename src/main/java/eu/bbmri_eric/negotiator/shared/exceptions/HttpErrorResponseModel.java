package eu.bbmri_eric.negotiator.shared.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Error response for REST API according to RFC 7807. */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HttpErrorResponseModel {
  private String type;
  private String title;
  private String detail;
  private int status;
}
