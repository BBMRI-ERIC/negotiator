package eu.bbmri_eric.negotiator.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class AttachmentTestHelper {

  private byte[] loadResourceOrDefault(String path, Supplier<byte[]> defaultSupplier)
      throws IOException {
    try (InputStream inputStream = AttachmentTestHelper.class.getResourceAsStream(path)) {
      if (inputStream == null) {
        return defaultSupplier.get();
      }
      return inputStream.readAllBytes();
    }
  }

  public byte[] loadTestDocxFile() throws IOException {
    return loadResourceOrDefault(
        "/test-documents/test.docx", AttachmentTestHelper::createMinimalDocxBytes);
  }

  public byte[] loadTestDocFileValid() throws IOException {
    return loadResourceOrDefault(
        "/test-documents/test-valid.doc", AttachmentTestHelper::createMinimalDocBytes);
  }

  public byte[] loadTestDocFile() throws IOException {
    return loadResourceOrDefault(
        "/test-documents/test.doc", AttachmentTestHelper::createMinimalDocBytes);
  }

  public byte[] createMinimalDocxBytes() {
    String minimalDocx =
        "PK\u0003\u0004\u0014\u0000\u0000\u0000\u0008\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0019\u0000\u0000\u0000[Content_Types].xmlPK\u0003\u0004\u0014\u0000"
            + "\u0000\u0000\u0008\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u000b\u0000\u0000\u0000"
            + "_rels/.relsPK\u0005\u0006\u0000\u0000\u0000\u0000\u0002\u0000\u0002\u0000"
            + "^\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000";
    return minimalDocx.getBytes();
  }

  public byte[] createMinimalDocBytes() {
    byte[] docHeader = {
      (byte) 0xD0,
      (byte) 0xCF,
      (byte) 0x11,
      (byte) 0xE0,
      (byte) 0xA1,
      (byte) 0xB1,
      (byte) 0x1A,
      (byte) 0xE1
    };
    byte[] docContent = new byte[512];
    System.arraycopy(docHeader, 0, docContent, 0, docHeader.length);
    return docContent;
  }

  public byte[] createValidDocBytes() {
    // Create a more complete DOC structure that might be parseable
    byte[] docHeader = {
      (byte) 0xD0,
      (byte) 0xCF,
      (byte) 0x11,
      (byte) 0xE0,
      (byte) 0xA1,
      (byte) 0xB1,
      (byte) 0x1A,
      (byte) 0xE1,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x3E,
      0x00,
      0x03,
      0x00,
      (byte) 0xFE,
      (byte) 0xFF,
      0x09,
      0x00,
      0x06,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x10,
      0x00,
      0x00
    };
    byte[] docContent = new byte[2048];
    System.arraycopy(docHeader, 0, docContent, 0, docHeader.length);
    return docContent;
  }

  public byte[] createValidEmptyDocBytes() {
    // Create a DOC structure that will parse but have no paragraphs
    byte[] docHeader = {
      (byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0,
      (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1
    };
    byte[] docContent = new byte[1024];
    System.arraycopy(docHeader, 0, docContent, 0, docHeader.length);
    return docContent;
  }

  public byte[] createRealisticDocBytes() {
    // Create a more realistic DOC file structure that has a better chance of being parsed
    // This creates a minimal but more complete OLE2 compound document structure
    byte[] docContent = new byte[4096]; // Larger size for more realistic structure

    // OLE2 header (first 512 bytes are the header sector)
    byte[] oleHeader = {
      // OLE signature
      (byte) 0xD0,
      (byte) 0xCF,
      (byte) 0x11,
      (byte) 0xE0,
      (byte) 0xA1,
      (byte) 0xB1,
      (byte) 0x1A,
      (byte) 0xE1,
      // Minor version
      0x00,
      0x00,
      // Major version
      0x3E,
      0x00,
      // Byte order
      (byte) 0xFE,
      (byte) 0xFF,
      // Sector size (512 bytes = 2^9)
      0x09,
      0x00,
      // Mini sector size (64 bytes = 2^6)
      0x06,
      0x00,
      // Reserved fields
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      // Number of directory sectors
      0x00,
      0x00,
      0x00,
      0x00,
      // Number of FAT sectors
      0x01,
      0x00,
      0x00,
      0x00,
      // Directory first sector
      0x01,
      0x00,
      0x00,
      0x00,
      // Transaction signature
      0x00,
      0x00,
      0x00,
      0x00,
      // Mini stream cutoff (4096 bytes)
      0x00,
      0x10,
      0x00,
      0x00,
      // First mini FAT sector
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      // Number of mini FAT sectors
      0x00,
      0x00,
      0x00,
      0x00,
      // First difat sector
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF,
      (byte) 0xFF
    };

    System.arraycopy(oleHeader, 0, docContent, 0, oleHeader.length);

    // Fill remaining header with appropriate values
    // FAT array (starting at offset 76)
    int fatOffset = 76;
    // Sector 0 points to sector 1 (continuation)
    docContent[fatOffset] = (byte) 0xFF;
    docContent[fatOffset + 1] = (byte) 0xFF;
    docContent[fatOffset + 2] = (byte) 0xFF;
    docContent[fatOffset + 3] = (byte) 0xFE; // End of chain

    // Add some realistic Word document content in subsequent sectors
    // This creates a minimal Word document structure
    int contentOffset = 512; // Start of sector 1
    String wordContent = "Microsoft Word Document Content";
    byte[] contentBytes = wordContent.getBytes();
    System.arraycopy(
        contentBytes,
        0,
        docContent,
        contentOffset,
        Math.min(contentBytes.length, docContent.length - contentOffset));

    return docContent;
  }

  public byte[] createValidXlsxBytes() throws IOException {
    return loadResourceOrDefault(
        "/test-documents/test.xlsx",
        () -> {
          try {
            return createMinimalValidXlsxBytes();
          } catch (IOException e) {
            throw new RuntimeException("Failed to generate fallback XLSX", e);
          }
        });
  }

  public byte[] createMinimalValidXlsxBytes() throws IOException {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
        new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
      org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");
      org.apache.poi.ss.usermodel.Row row = sheet.createRow(0);
      org.apache.poi.ss.usermodel.Cell cell = row.createCell(0);
      cell.setCellValue("Test");
      workbook.write(baos);
    }
    return baos.toByteArray();
  }

  public byte[] createEmptyXlsxBytes() throws IOException {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
        new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
      workbook.createSheet("EmptySheet");
      workbook.write(baos);
    }
    return baos.toByteArray();
  }

  public byte[] createXlsxWithMultipleSheets() throws IOException {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
        new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
      org.apache.poi.ss.usermodel.Sheet sheet1 = workbook.createSheet("Sheet1");
      org.apache.poi.ss.usermodel.Row row1 = sheet1.createRow(0);
      row1.createCell(0).setCellValue("Sheet 1 Data");
      row1.createCell(1).setCellValue("Column 2");

      org.apache.poi.ss.usermodel.Sheet sheet2 = workbook.createSheet("Sheet2");
      org.apache.poi.ss.usermodel.Row row2 = sheet2.createRow(0);
      row2.createCell(0).setCellValue("Sheet 2 Data");

      workbook.write(baos);
    }
    return baos.toByteArray();
  }

  public byte[] createXlsxWithFormulas() throws IOException {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
        new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
      org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Formulas");
      org.apache.poi.ss.usermodel.Row row1 = sheet.createRow(0);
      row1.createCell(0).setCellValue(10);
      row1.createCell(1).setCellValue(20);

      org.apache.poi.ss.usermodel.Row row2 = sheet.createRow(1);

      org.apache.poi.ss.usermodel.Cell formulaCell = row2.createCell(0);
      formulaCell.setCellFormula("SUM(A1:B1)");

      workbook.write(baos);
    }
    return baos.toByteArray();
  }

  public byte[] createXlsxWithVariousCellTypes() throws IOException {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
        new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
      org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Types");
      org.apache.poi.ss.usermodel.Row row = sheet.createRow(0);

      row.createCell(0).setCellValue("Text");
      row.createCell(1).setCellValue(123.45);
      row.createCell(2).setCellValue(true);
      org.apache.poi.ss.usermodel.Cell formulaCell = row.createCell(3);
      formulaCell.setCellFormula("B1*2");
      row.createCell(4).setBlank();

      workbook.write(baos);
    }
    return baos.toByteArray();
  }

  public byte[] createXlsxWithLongText() throws IOException {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
        new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
      org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("LongText");
      org.apache.poi.ss.usermodel.Row row = sheet.createRow(0);

      String longText =
          "This is a very long text that should be truncated when rendering to PDF because it"
              + " exceeds the maximum width allowed for a cell in the PDF output format and we"
              + " need to test the truncation logic";
      row.createCell(0).setCellValue(longText);

      workbook.write(baos);
    }
    return baos.toByteArray();
  }

  public byte[] createXlsxWithShortText() throws IOException {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
        new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
      org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("ShortText");
      org.apache.poi.ss.usermodel.Row row = sheet.createRow(0);
      row.createCell(0).setCellValue("Short");
      workbook.write(baos);
    }
    return baos.toByteArray();
  }

  public byte[] createXlsxWithManyRows() throws IOException {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
        new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
      org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("ManyRows");

      for (int i = 0; i < 60; i++) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(i);
        row.createCell(0).setCellValue("Row " + i);
        row.createCell(1).setCellValue(i * 10);
      }

      workbook.write(baos);
    }
    return baos.toByteArray();
  }

  public byte[] createXlsxWithEmptyCells() throws IOException {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
        new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
      org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("EmptyCells");
      org.apache.poi.ss.usermodel.Row row1 = sheet.createRow(0);

      row1.createCell(0).setCellValue("A1");
      row1.createCell(2).setCellValue("C1");

      org.apache.poi.ss.usermodel.Row row2 = sheet.createRow(1);

      row2.createCell(1).setCellValue("B2");
      row2.createCell(3).setCellValue("");

      workbook.write(baos);
    }
    return baos.toByteArray();
  }

  public byte[] createXlsxWithSingleColumn() throws IOException {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
        new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
      org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("SingleColumn");

      for (int i = 0; i < 5; i++) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(i);
        row.createCell(0).setCellValue("Value " + i);
      }

      workbook.write(baos);
    }
    return baos.toByteArray();
  }

  public byte[] createXlsxWithBooleanAndBlankCells() throws IOException {
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
        new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
      org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("BooleanBlank");
      org.apache.poi.ss.usermodel.Row row1 = sheet.createRow(0);

      row1.createCell(0).setCellValue(true);
      row1.createCell(1).setCellValue(false);
      row1.createCell(2).setBlank();

      org.apache.poi.ss.usermodel.Row row2 = sheet.createRow(1);
      row2.createCell(0).setCellValue("After Boolean");
      row2.createCell(1).setBlank();
      row2.createCell(2).setCellValue(true);

      workbook.write(baos);
    }
    return baos.toByteArray();
  }
}
