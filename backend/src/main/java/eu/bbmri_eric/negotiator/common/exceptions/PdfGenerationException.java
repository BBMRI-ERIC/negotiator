package eu.bbmri_eric.negotiator.common.exceptions;

public class PdfGenerationException extends RuntimeException {

  private static final String error_message =
          "The PDF generation process failed. This could be due to an issue with the template or the data provided.";

  public PdfGenerationException() {
    super(error_message);
  }

  public PdfGenerationException(String message) {super(message);}
}
