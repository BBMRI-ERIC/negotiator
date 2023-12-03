package eu.bbmri.eric.csit.service.negotiator.exceptions;

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
public class RestError {
  private String type;
  private String title;
  private String detail;
  private int status;
}
