package eu.bbmri_eric.negotiator.exceptions;

public class StorageException extends RuntimeException {

  public StorageException(String message) {
    super(message);
  }

  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
