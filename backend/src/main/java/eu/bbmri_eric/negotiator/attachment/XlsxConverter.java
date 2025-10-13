package eu.bbmri_eric.negotiator.attachment;

import eu.bbmri_eric.negotiator.common.exceptions.PdfGenerationException;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Converter for XLSX (Excel) files to PDF format. */
@CommonsLog
class XlsxConverter implements FileTypeConverter {
  /** Page margin in points. */
  private static final float MARGIN = 50;

  /** Font size for cell text. */
  private static final float FONT_SIZE = 10;

  /** Line leading (spacing between rows). */
  private static final float LEADING = 14;

  /** Padding inside cells. */
  private static final float CELL_PADDING = 5;

  /** Minimum width for columns. */
  private static final float MIN_COLUMN_WIDTH = 60;

  @Override
  public byte[] convertToPdf(final byte[] xlsxBytes) throws IOException, PdfGenerationException {
    if (xlsxBytes == null || xlsxBytes.length == 0) {
      throw new IllegalArgumentException("Input XLSX bytes are null or empty");
    }

    log.debug("Converting XLSX to PDF, input size: " + xlsxBytes.length);

    try (ByteArrayInputStream xlsxInputStream = new ByteArrayInputStream(xlsxBytes);
        Workbook workbook = new XSSFWorkbook(xlsxInputStream);
        PDDocument document = new PDDocument();
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {

      // Process each sheet in the workbook
      for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
        Sheet sheet = workbook.getSheetAt(sheetIndex);
        if (sheet.getPhysicalNumberOfRows() > 0) {
          convertSheetToPdf(document, sheet);
        }
      }

      if (document.getNumberOfPages() == 0) {
        // Add at least one page if workbook is empty
        document.addPage(new PDPage(PDRectangle.A4));
      }

      document.save(pdfOutputStream);
      byte[] result = pdfOutputStream.toByteArray();
      log.debug("Successfully converted XLSX to PDF, output size: " + result.length);
      return result;
    } catch (Exception e) {
      log.error("Error converting XLSX to PDF: " + e.getMessage(), e);
      throw new PdfGenerationException();
    }
  }

  private void convertSheetToPdf(final PDDocument document, final Sheet sheet) throws IOException {
    PDPage page = new PDPage(PDRectangle.A4);
    document.addPage(page);

    try (PDPageContentStream contentStream =
        new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {

      float yPosition = page.getMediaBox().getHeight() - MARGIN;
      float pageWidth = page.getMediaBox().getWidth() - (2 * MARGIN);

      // Draw sheet name
      contentStream.beginText();
      contentStream.setFont(
          new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE + 2);
      contentStream.newLineAtOffset(MARGIN, yPosition);
      contentStream.showText("Sheet: " + sheet.getSheetName());
      contentStream.endText();
      yPosition -= LEADING * 2;

      // Calculate column widths
      int maxCols = 0;
      for (Row row : sheet) {
        if (row.getLastCellNum() > maxCols) {
          maxCols = row.getLastCellNum();
        }
      }

      float columnWidth = maxCols > 0 ? pageWidth / maxCols : MIN_COLUMN_WIDTH;
      columnWidth = Math.max(columnWidth, MIN_COLUMN_WIDTH);

      // Process rows
      for (Row row : sheet) {
        // Check if we need a new page
        if (yPosition < MARGIN + LEADING) {
          // Simplified version: stop processing when page is full
          // In production, you'd want to continue on a new page
          break;
        }

        // Draw row cells
        float xPosition = MARGIN;
        for (int cellIndex = 0; cellIndex < maxCols; cellIndex++) {
          Cell cell = row.getCell(cellIndex);
          String cellValue = getCellValueAsString(cell);

          // Draw cell border
          contentStream.setStrokingColor(Color.LIGHT_GRAY);
          contentStream.addRect(xPosition, yPosition - LEADING, columnWidth, LEADING);
          contentStream.stroke();

          // Draw cell text
          if (cellValue != null && !cellValue.isEmpty()) {
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE);
            contentStream.newLineAtOffset(xPosition + CELL_PADDING, yPosition - FONT_SIZE);

            // Truncate text if too long
            String displayText = truncateText(cellValue, columnWidth - (2 * CELL_PADDING));
            contentStream.showText(displayText);
            contentStream.endText();
          }

          xPosition += columnWidth;
        }

        yPosition -= LEADING;
      }
    }
  }

  private String getCellValueAsString(final Cell cell) {
    if (cell == null) {
      return "";
    }

    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> String.valueOf(cell.getNumericCellValue());
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> cell.getCellFormula();
      case BLANK -> "";
      default -> "";
    };
  }

  private String truncateText(final String text, final float maxWidth) {
    // Simple truncation - measure actual text width in production
    int maxChars = (int) (maxWidth / (FONT_SIZE * 0.6));
    if (text.length() > maxChars) {
      return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }
    return text;
  }
}
