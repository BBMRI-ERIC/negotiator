package eu.bbmri_eric.negotiator.negotiation.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

/** Utility class for merging multiple PDF documents into a single PDF. */
@CommonsLog
class PdfMerger {
  /**
   * Merges a list of PDF documents represented as byte arrays into a single PDF.
   *
   * @param pdfBytesList List of PDF documents as byte arrays.
   * @return The merged PDF as a byte array.
   * @throws IOException if an I/O error occurs during merging.
   */
  public static byte[] mergePdfs(List<byte[]> pdfBytesList) throws IOException {

    try (PDDocument mergedPdf = new PDDocument()) {
      for (byte[] pdfBytes : pdfBytesList) {
        if (pdfBytes == null || pdfBytes.length == 0) {
          continue;
        }

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
          mergeDocument(mergedPdf, doc);
        } catch (IOException e) {
          log.error("Failed to merge PDF document: " + e.getMessage(), e);
          try (PDDocument errorDoc = new PDDocument()) {
            errorDoc.addPage(new PDPage());
            errorDoc.getDocumentInformation().setTitle("Error Document");
            errorDoc
                .getDocumentInformation()
                .setSubject("An error occurred while merging PDF documents.");
            mergeDocument(mergedPdf, errorDoc);
          }
        }
      }

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      mergedPdf.save(outputStream);
      return outputStream.toByteArray();
    }
  }

  private static void mergeDocument(PDDocument mergedPdf, PDDocument document) throws IOException {
    PDFMergerUtility merger = new PDFMergerUtility();
    merger.appendDocument(mergedPdf, document);
  }
}
