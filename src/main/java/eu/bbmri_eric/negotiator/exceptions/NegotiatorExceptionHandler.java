package eu.bbmri_eric.negotiator.exceptions;

import eu.bbmri_eric.negotiator.dto.error.ErrorResponse;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationRequestParameters;
import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.LazyInitializationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.jwt.JwtDecoderInitializationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
@CommonsLog
public class NegotiatorExceptionHandler {

  @ExceptionHandler(JwtDecoderInitializationException.class)
  public final ResponseEntity<HttpErrorResponseModel> handleJwtDecoderError(
      RuntimeException ex, WebRequest request) {
    HttpErrorResponseModel errorResponse =
        HttpErrorResponseModel.builder()
            .title("Authentication Failure")
            .detail("We could not decode the JWT token. Please try again later.")
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ServletException.class)
  public final ResponseEntity<HttpErrorResponseModel> handleServletError(
      RuntimeException ex, WebRequest request) {
    HttpErrorResponseModel errorResponse =
        HttpErrorResponseModel.builder()
            .title("Internal server error")
            .detail("An unspecified error occurred.")
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
    String message;
    if (ex.getBindingResult().getTarget() instanceof NegotiationRequestParameters) {
      message = "One or more query parameters are not valid";
    } else {
      message = "The body of the Negotiation is not valid";
    }
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
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
      RuntimeException ex, WebRequest request) {
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
      RuntimeException ex, WebRequest request) {
    HttpErrorResponseModel errorResponse =
        HttpErrorResponseModel.builder()
            .title("Unable to sort by provided property.")
            .detail(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(UnsupportedFilterException.class)
  public final ResponseEntity<HttpErrorResponseModel> handleUnsupportedFilterException(
      RuntimeException ex, WebRequest request) {
    HttpErrorResponseModel errorResponse =
        HttpErrorResponseModel.builder()
            .title("Unable to filter by provided property.")
            .detail(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NotImplementedException.class)
  public final ResponseEntity<HttpErrorResponseModel> handleNotImplementedException() {
    HttpErrorResponseModel errorResponse =
        HttpErrorResponseModel.builder()
            .title("Endpoint not yet implemented")
            .detail("Sorry this endpoint is still under development. Try again later")
            .status(HttpStatus.NOT_IMPLEMENTED.value())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_IMPLEMENTED);
  }

  @ExceptionHandler(LazyInitializationException.class)
  public final ResponseEntity<HttpErrorResponseModel> handleLazyInitializationException(
      RuntimeException ex, WebRequest request) {
    log.error(
        "Lazy initialization failure. Check transaction management configuration."
            + ex.getMessage());
    log.error(Arrays.toString(ex.getStackTrace()));
    HttpErrorResponseModel errorResponse =
        HttpErrorResponseModel.builder()
            .title("A database error occurred.")
            .detail("There was en error fetching data from the database. Please try again later.")
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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

  @ExceptionHandler(WrongJWTException.class)
  public final ResponseEntity<HttpErrorResponseModel> handleJWTError(
      RuntimeException ex, WebRequest request) {
    HttpErrorResponseModel errorResponse =
        HttpErrorResponseModel.builder()
            .title("Authentication error")
            .detail(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(JwtValidationException.class)
  public final ResponseEntity<HttpErrorResponseModel> handleInvalidBearerTokenException(
      RuntimeException ex, WebRequest request) {
    HttpErrorResponseModel errorResponse =
        HttpErrorResponseModel.builder()
            .title("Authorization error")
            .detail(ex.getMessage())
            .status(HttpStatus.UNAUTHORIZED.value())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }
}
