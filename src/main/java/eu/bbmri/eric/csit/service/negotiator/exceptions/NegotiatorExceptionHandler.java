package eu.bbmri.eric.csit.service.negotiator.exceptions;

import eu.bbmri.eric.csit.service.negotiator.dto.error.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoderInitializationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import javax.validation.ConstraintViolationException;

@ControllerAdvice
public class NegotiatorExceptionHandler {

  @ExceptionHandler(JwtDecoderInitializationException.class)
  public final ResponseEntity<ErrorResponse> handleJwtDecoderError(
      RuntimeException ex, WebRequest request) {
    ErrorResponse errorResponse =
        new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Cannot verify the JWT token");
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public final ResponseEntity<ErrorResponse> handleRequestValidationException(
      MethodArgumentNotValidException ex, WebRequest request) {
    ErrorResponse errorResponse =
        new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
            "The body of the Negotiation is not valid");
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public final ResponseEntity<ErrorResponse> handleEntityNotFoundException(
      EntityNotFoundException ex, WebRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler({
      EntityNotStorableException.class,
      WrongRequestException.class,
      ConstraintViolationException.class
  })
  public final ResponseEntity<ErrorResponse> handleBadRequestExceptions(
      RuntimeException ex, WebRequest request) {
    ErrorResponse errorResponse =
        new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }
}
