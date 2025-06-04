package eu.bbmri_eric.negotiator.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;


public class PdfMerger {
    public static byte[] mergePdfs(List<byte[]> pdfBytesList) throws IOException {
        PDDocument mergedPdf = new PDDocument();

        try {
            for (byte[] pdfBytes : pdfBytesList) {
                PDDocument doc = Loader.loadPDF(pdfBytes);
                mergeDocument(mergedPdf, doc);
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
