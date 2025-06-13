package eu.bbmri_eric.negotiator.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

@CommonsLog
public class PdfMerger {
  public static byte[] mergePdfs(List<byte[]> pdfBytesList) throws IOException {
    PDDocument mergedPdf = new PDDocument();

    try {
      for (byte[] pdfBytes : pdfBytesList) {
        if (pdfBytes == null || pdfBytes.length == 0) {
          continue;
        }
        PDDocument doc = new PDDocument();
        try {
          doc = Loader.loadPDF(pdfBytes);
          mergeDocument(mergedPdf, doc);
        } catch (IOException e) {
          log.error("Failed to merge PDF document: " + e.getMessage(), e);
          doc.addPage(new PDPage());
          doc.getDocumentInformation().setTitle("Error Document");
          doc.getDocumentInformation().setSubject("An error occurred while merging PDF documents.");
          mergeDocument(mergedPdf, doc);
        } finally {
          doc.close();
        }
      }

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      mergedPdf.save(outputStream);
      return outputStream.toByteArray();
    } finally {
      mergedPdf.close();
    }
  }

  private static void mergeDocument(PDDocument mergedPdf, PDDocument document) throws IOException {
    PDFMergerUtility merger = new PDFMergerUtility();
    merger.appendDocument(mergedPdf, document);
  }
}
