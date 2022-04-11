package eu.bbmri.eric.csit.service.negotiator.dto;

public class ErrorResponse {

  private final Integer code;

  private final String message;

  public ErrorResponse(Integer code, String message) {
    this.code = code;
    this.message = message;
  }

  public Integer getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
