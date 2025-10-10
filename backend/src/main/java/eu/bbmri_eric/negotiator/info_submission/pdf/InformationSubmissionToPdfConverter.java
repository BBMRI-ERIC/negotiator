package eu.bbmri_eric.negotiator.info_submission.pdf;

import com.lowagie.text.DocumentException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * Service for converting CSV data to PDF format using Thymeleaf templates and Flying Saucer PDF
 * renderer.
 */
@Service
@CommonsLog
public class InformationSubmissionToPdfConverter {

  private static final String DEFAULT_INFORMATION_SUBMISSION_PDF_TEMPLATE_NAME =
      "INFORMATION_SUBMISSION_SUMMARY";

  private final TemplateEngine templateEngine;

  public InformationSubmissionToPdfConverter(TemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  /**
   * Converts CSV file content to a PDF document.
   *
   * @param csvFile the CSV file as a MultipartFile
   * @param negotiationId the negotiation ID
   * @param requirementName the requirement name
   * @return byte array containing the PDF document
   * @throws IOException if there's an error reading the CSV or generating the PDF
   */
  public byte[] convertCsvToPdf(MultipartFile csvFile, String negotiationId, String requirementName)
      throws IOException {
    CsvData csvData = parseCsv(csvFile);

    Map<String, Object> variables = new HashMap<>();
    variables.put("negotiationId", negotiationId);
    variables.put("requirementName", requirementName);
    variables.put("headers", csvData.headers());
    variables.put("rows", csvData.rows());
    variables.put("totalSubmissions", csvData.rows().size());
    variables.put(
        "generatedDate",
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

    return generatePdfFromTemplate(variables);
  }

  /**
   * Parses CSV content into headers and rows.
   *
   * @param csvFile the CSV file to parse
   * @return CsvData containing headers and rows
   * @throws IOException if there's an error reading the file
   */
  private CsvData parseCsv(MultipartFile csvFile) throws IOException {
    String csvContent = new String(csvFile.getBytes());
    String[] lines = csvContent.split("\n");

    if (lines.length == 0) {
      return new CsvData(new ArrayList<>(), new ArrayList<>());
    }

    // Parse headers
    List<String> headers = parseCsvLine(lines[0]);

    // Parse rows
    List<List<String>> rows = new ArrayList<>();
    for (int i = 1; i < lines.length; i++) {
      if (!lines[i].trim().isEmpty()) {
        List<String> row = parseCsvLine(lines[i]);
        // Ensure row has same length as headers
        while (row.size() < headers.size()) {
          row.add("");
        }
        rows.add(row);
      }
    }

    return new CsvData(headers, rows);
  }

  /**
   * Parses a single CSV line, handling quoted values and commas.
   *
   * @param line the CSV line to parse
   * @return list of values from the line
   */
  private List<String> parseCsvLine(String line) {
    List<String> values = new ArrayList<>();
    StringBuilder currentValue = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);

      if (c == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          // Escaped quote
          currentValue.append('"');
          i++; // Skip next quote
        } else {
          // Toggle quote state
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        // End of value
        values.add(currentValue.toString().trim());
        currentValue = new StringBuilder();
      } else {
        currentValue.append(c);
      }
    }

    // Add last value
    values.add(currentValue.toString().trim());

    return values;
  }

  /**
   * Generates a PDF from the Thymeleaf template with the provided variables.
   *
   * @param variables the template variables
   * @return byte array containing the PDF
   * @throws IOException if there's an error generating the PDF
   */
  private byte[] generatePdfFromTemplate(Map<String, Object> variables) throws IOException {
    Context context = new Context();
    context.setVariables(variables);

    // String htmlContent = templateEngine.process("INFORMATION_SUBMISSION_SUMMARY.html", context);

    String htmlContent =
        templateEngine
            .process(DEFAULT_INFORMATION_SUBMISSION_PDF_TEMPLATE_NAME, context)
            .replaceAll("(<br />)+$", "");

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      ITextRenderer renderer = new ITextRenderer();
      renderer.setDocumentFromString(htmlContent);
      renderer.layout();
      renderer.createPDF(outputStream);
      return outputStream.toByteArray();
    } catch (DocumentException e) {
      log.error("Failed to generate PDF from template", e);
      throw new IOException("Failed to generate PDF document", e);
    }
  }

  /** Record to hold CSV data structure. */
  private record CsvData(List<String> headers, List<List<String>> rows) {}
}
