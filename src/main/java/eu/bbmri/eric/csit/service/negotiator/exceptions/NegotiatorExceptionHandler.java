package eu.bbmri.eric.csit.service.negotiator.exceptions;

import eu.bbmri.eric.csit.service.negotiator.dto.error.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.jwt.JwtDecoderInitializationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class NegotiatorExceptionHandler {

  @ExceptionHandler(JwtDecoderInitializationException.class)
  public final ResponseEntity<HttpErrorResponseModel> handleJwtDecoderError(
      RuntimeException ex, WebRequest request) {
    HttpErrorResponseModel errorResponse =
        HttpErrorResponseModel.builder()
            .title("Authentication Failure")
            .detail("We could not decode the JWT token. PLease try again later.")
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(AuthenticationServiceException.class)
  public final ResponseEntity<HttpErrorResponseModel> handleJwtError(
      RuntimeException ex, WebRequest request) {
    HttpErrorResponseModel errorResponse =
        HttpErrorResponseModel.builder()
            .type("https://www.rfc-editor.org/rfc/rfc9110#status.500")
            .title("Authentication Failure")
            .detail("We could not reach the authorization server. PLease try again later.")
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public final ResponseEntity<ErrorResponse> handleRequestValidationException(
      MethodArgumentNotValidException ex, WebRequest request) {
    ErrorResponse errorResponse =
        new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(), "The body of the Negotiation is not valid");
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public final ResponseEntity<ErrorResponse> handleEntityNotFoundException(
      EntityNotFoundException ex, WebRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public final ResponseEntity<HttpErrorResponseModel> handleUserNotFoundException(
      EntityNotFoundException ex, WebRequest request) {
    HttpErrorResponseModel errorResponse =
        HttpErrorResponseModel.builder()
            .title("User not found.")
            .detail(ex.getMessage())
            .status(HttpStatus.NOT_FOUND.value())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(WrongSortingPropertyException.class)
  public final ResponseEntity<HttpErrorResponseModel> handleWrongSortingPropertyException(
      EntityNotFoundException ex, WebRequest request) {
    HttpErrorResponseModel errorResponse =
        HttpErrorResponseModel.builder()
            .title("Unable to sort by provided property.")
            .detail(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({
    EntityNotStorableException.class,
    WrongRequestException.class,
    ConstraintViolationException.class,
    MaxUploadSizeExceededException.class
  })
  public final ResponseEntity<ErrorResponse> handleBadRequestExceptions(
      RuntimeException ex, WebRequest request) {
    ErrorResponse errorResponse =
        new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({ForbiddenRequestException.class})
  public final ResponseEntity<ErrorResponse> handleForbiddenException(
      ForbiddenRequestException ex, WebRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler({IllegalArgumentException.class})
  public final ResponseEntity<HttpErrorResponseModel> handleIllegalArgument(
      IllegalArgumentException ex, WebRequest request) {
    HttpErrorResponseModel errorResponse =
        HttpErrorResponseModel.builder()
            .title("Bad request.")
            .detail(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }
}
