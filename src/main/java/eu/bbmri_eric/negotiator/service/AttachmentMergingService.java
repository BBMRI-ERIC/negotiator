package eu.bbmri_eric.negotiator.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import eu.bbmri_eric.negotiator.dto.attachments.AttachmentDTO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;


@Service
public class AttachmentMergingService {

    private final AttachmentService attachmentService;

    public AttachmentMergingService(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * Merge attachments to a PDF file.
     *
     * @param attachmentIds The list of attachment IDs to merge.
     * @return The merged PDF file.
     */
    public byte[] mergeAttachmentsToPdf(List<String> attachmentIds) {
        List<AttachmentDTO> attachmentsList =
                attachmentIds.stream()
                        .map(id -> attachmentService.findById(id))
                        .collect(Collectors.toList());

        List<byte[]> convertedAttachments = convertAttachmentsToPdf(attachmentsList);
        byte[] mergedPdf = null;
        try {
            mergedPdf = PdfMergerService.mergePdfs(convertedAttachments);
        } catch (IOException e) {
            throw new RuntimeException("Failed to merge attachments to PDF", e);
        }

        return mergedPdf;
    }

    private List<byte[]> convertAttachmentsToPdf(List<AttachmentDTO> attachmentsList) {
        return attachmentsList.stream()
                .map(
                        attachmentDTO -> {
                            try {
                                if (attachmentDTO.getContentType().equals("application/pdf")) {
                                    return attachmentDTO.getPayload();
                                } else if (attachmentDTO
                                        .getContentType()
                                        .equals(
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                                    return convertDocxToPdf(attachmentDTO.getPayload());
                                } else if (attachmentDTO.getContentType().equals("application/msword")) {
                                    return convertDocToPdf(attachmentDTO.getPayload());
                                } else {
                                    return null;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        })
                .filter(pdfBytes -> pdfBytes != null)
                .collect(Collectors.toList());
    }

    private byte[] convertDocToPdf(byte[] docBytes) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(docBytes));
             ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
             PDDocument pdfDoc = new PDDocument()) {

            PDPage page = new PDPage();
            pdfDoc.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(pdfDoc, page);
                 WordExtractor extractor = new WordExtractor(doc)) {

                Range range = doc.getRange();
                contentStream.beginText();
                contentStream.setLeading(14.5f);
                contentStream.newLineAtOffset(25, 725);

                for (int i = 0; i < range.numParagraphs(); i++) {
                    Paragraph paragraph = range.getParagraph(i);
                    for (int j = 0; j < paragraph.numCharacterRuns(); j++) {
                        CharacterRun run = paragraph.getCharacterRun(j);
                        String text = run.text();

                        int fontSize = run.getFontSize() / 2;
                        String fontFamily = run.getFontName();
                        InputStream fontStream = getClass().getResourceAsStream("/fonts/" + fontFamily + ".ttf");
                        PDType0Font pdfFont = PDType0Font.load(pdfDoc, fontStream);

                        contentStream.setFont(pdfFont, fontSize);
                        contentStream.showText(text.trim());
                        contentStream.newLineAtOffset(0, -fontSize - 2);
                    }
                }

                contentStream.endText();
            }

            pdfDoc.save(pdfOutputStream);
            return pdfOutputStream.toByteArray();
        }
    }

    private byte[] convertDocxToPdf(byte[] docxBytes) throws Exception {
        Document pdfDoc = null;
        try (ByteArrayInputStream docxInputStream = new ByteArrayInputStream(docxBytes);
             ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {

            XWPFDocument docx = new XWPFDocument(docxInputStream);
            List<XWPFParagraph> paragraphs = docx.getParagraphs();
            PdfOptions options = PdfOptions.create();
            pdfDoc = new Document();
            PdfConverter.getInstance().convert(docx, pdfOutputStream, options);
//            PdfWriter.getInstance(pdfDoc, pdfOutputStream);
//            pdfDoc.open();
//            for (XWPFParagraph paragraph : paragraphs) {
//                pdfDoc.add(new com.lowagie.text.Paragraph(paragraph.getText()));
//            }
//            pdfDoc.close();
            return pdfOutputStream.toByteArray();
        } finally {
            if (pdfDoc != null) {
                pdfDoc.close();
            }
        }
//        try (ByteArrayInputStream docxInputStream = new ByteArrayInputStream(docxBytes);
//             ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {
//
//            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(docxInputStream);
//            PdfConversion conversion = Docx4J.toPDF(wordMLPackage);
//            conversion.output(pdfOutputStream, new PdfConversion.PdfSettings());
//
//            return pdfOutputStream.toByteArray();
//        }
    }
}
