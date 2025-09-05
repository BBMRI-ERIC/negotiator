package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.attachment.AttachmentConversionService;
import eu.bbmri_eric.negotiator.attachment.AttachmentService;
import eu.bbmri_eric.negotiator.attachment.dto.AttachmentMetadataDTO;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
class NegotiationPdfGenerationTest {

  private static final String NEGOTIATION_1_ID = "negotiation-1";
  private static final String PDF_ENDPOINT = "/v3/negotiations/{id}/pdf";
  private static final String FULL_PDF_ENDPOINT =
      "/v3/negotiations/{id}/pdf?includeAttachments=true";
  private static final String ATTACHMENT_ENDPOINT = "/v3/negotiations/{id}/attachments";

  @Autowired private WebApplicationContext context;
  @Autowired private AttachmentService attachmentService;
  @Autowired private AttachmentConversionService conversionService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  @WithMockNegotiatorUser(
      id = 109L,
      authorities = {"ROLE_ADMIN"})
  void testGenerateNegotiationPdf_WithoutAttachments_ReturnsBasicPdf() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get(PDF_ENDPOINT, NEGOTIATION_1_ID)
                    .with(jwt().jwt(builder -> builder.subject("109"))))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        "attachment; filename=\"negotiation-" + NEGOTIATION_1_ID + ".pdf\""))
            .andReturn();

    byte[] pdfContent = result.getResponse().getContentAsByteArray();
    assertNotNull(pdfContent);
    assertTrue(pdfContent.length > 0);
    assertTrue(new String(pdfContent).startsWith("%PDF"));
  }

  @Test
  @WithMockNegotiatorUser(
      id = 109L,
      authorities = {"ROLE_ADMIN"})
  void testGenerateNegotiationPdfWithAttachments_WithPdfAttachment_MergesPdfs() throws Exception {
    MockMultipartFile pdfFile =
        new MockMultipartFile("file", "test.pdf", "application/pdf", createSimplePdfBytes());

    mockMvc
        .perform(
            multipart(ATTACHMENT_ENDPOINT, NEGOTIATION_1_ID)
                .file(pdfFile)
                .with(jwt().jwt(builder -> builder.subject("109"))))
        .andExpect(status().isCreated());

    MvcResult result =
        mockMvc
            .perform(
                get(FULL_PDF_ENDPOINT, NEGOTIATION_1_ID)
                    .with(jwt().jwt(builder -> builder.subject("109"))))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        "attachment; filename=\"negotiation-" + NEGOTIATION_1_ID + "-merged.pdf\""))
            .andReturn();

    byte[] mergedPdfContent = result.getResponse().getContentAsByteArray();
    assertNotNull(mergedPdfContent);
    assertTrue(mergedPdfContent.length > 0);
    assertTrue(new String(mergedPdfContent).startsWith("%PDF"));
  }

  @Test
  @WithMockNegotiatorUser(
      id = 109L,
      authorities = {"ROLE_ADMIN"})
  void testGenerateNegotiationPdfWithAttachments_WithDocxAttachment_ConvertsAndMerges()
      throws Exception {
    MockMultipartFile docxFile =
        new MockMultipartFile(
            "file",
            "test.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            createMinimalDocxBytes());

    mockMvc
        .perform(
            multipart(ATTACHMENT_ENDPOINT, NEGOTIATION_1_ID)
                .file(docxFile)
                .with(jwt().jwt(builder -> builder.subject("109"))))
        .andExpect(status().isCreated());

    MvcResult result =
        mockMvc
            .perform(
                get(FULL_PDF_ENDPOINT, NEGOTIATION_1_ID)
                    .with(jwt().jwt(builder -> builder.subject("109"))))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn();

    byte[] mergedPdfContent = result.getResponse().getContentAsByteArray();
    assertNotNull(mergedPdfContent);
    assertTrue(mergedPdfContent.length > 0);
  }

  @Test
  @WithMockNegotiatorUser(
      id = 109L,
      authorities = {"ROLE_ADMIN"})
  void testGenerateNegotiationPdfWithAttachments_WithDocAttachment_ConvertsAndMerges()
      throws Exception {
    MockMultipartFile docFile =
        new MockMultipartFile("file", "test.doc", "application/msword", createMinimalDocBytes());

    mockMvc
        .perform(
            multipart(ATTACHMENT_ENDPOINT, NEGOTIATION_1_ID)
                .file(docFile)
                .with(jwt().jwt(builder -> builder.subject("109"))))
        .andExpect(status().isCreated());

    MvcResult result =
        mockMvc
            .perform(
                get(FULL_PDF_ENDPOINT, NEGOTIATION_1_ID)
                    .with(jwt().jwt(builder -> builder.subject("109"))))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn();

    byte[] mergedPdfContent = result.getResponse().getContentAsByteArray();
    assertNotNull(mergedPdfContent);
    assertTrue(mergedPdfContent.length > 0);
  }

  @Test
  @WithMockNegotiatorUser(
      id = 109L,
      authorities = {"ROLE_ADMIN"})
  void testGenerateNegotiationPdfWithAttachments_WithMultipleAttachments_MergesAll()
      throws Exception {
    MockMultipartFile pdfFile =
        new MockMultipartFile("file", "test.pdf", "application/pdf", createSimplePdfBytes());

    MockMultipartFile docxFile =
        new MockMultipartFile(
            "file",
            "test.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            createMinimalDocxBytes());

    mockMvc
        .perform(
            multipart(ATTACHMENT_ENDPOINT, NEGOTIATION_1_ID)
                .file(pdfFile)
                .with(jwt().jwt(builder -> builder.subject("109"))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            multipart(ATTACHMENT_ENDPOINT, NEGOTIATION_1_ID)
                .file(docxFile)
                .with(jwt().jwt(builder -> builder.subject("109"))))
        .andExpect(status().isCreated());

    List<AttachmentMetadataDTO> attachments = attachmentService.findByNegotiation(NEGOTIATION_1_ID);
    assertTrue(attachments.size() >= 2);

    MvcResult result =
        mockMvc
            .perform(
                get(FULL_PDF_ENDPOINT, NEGOTIATION_1_ID)
                    .with(jwt().jwt(builder -> builder.subject("109"))))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn();

    byte[] mergedPdfContent = result.getResponse().getContentAsByteArray();
    assertNotNull(mergedPdfContent);
    assertTrue(mergedPdfContent.length > pdfFile.getSize() + docxFile.getSize());
  }

  @Test
  @WithMockNegotiatorUser(
      id = 109L,
      authorities = {"ROLE_ADMIN"})
  void testAttachmentConversionService_DirectlyTestsConversionLogic() throws Exception {
    MockMultipartFile pdfFile =
        new MockMultipartFile("file", "test.pdf", "application/pdf", loadTestFile("test.pdf"));

    MockMultipartFile docxFile =
        new MockMultipartFile(
            "file",
            "test.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            loadTestFile("test.docx"));

    AttachmentMetadataDTO pdfAttachment =
        attachmentService.createForNegotiation(NEGOTIATION_1_ID, null, pdfFile);
    AttachmentMetadataDTO docxAttachment =
        attachmentService.createForNegotiation(NEGOTIATION_1_ID, null, docxFile);

    List<String> attachmentIds = List.of(pdfAttachment.getId(), docxAttachment.getId());

    List<byte[]> convertedPdfs = conversionService.listToPdf(attachmentIds);

    assertEquals(2, convertedPdfs.size());
    assertNotNull(convertedPdfs.get(0));
    assertNotNull(convertedPdfs.get(1));
    assertTrue(convertedPdfs.get(0).length > 0);
    assertTrue(convertedPdfs.get(1).length > 0);
  }

  private byte[] createSimplePdfBytes() {
    StringBuilder pdf = new StringBuilder();
    pdf.append("%PDF-1.4\n");
    pdf.append("1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n");
    pdf.append("2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n");
    pdf.append(
        "3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R"
            + "/Resources<</Font<</F1 4 0 R>>>>/Contents 5 0 R>>endobj\n");
    pdf.append("4 0 obj<</Type/Font/Subtype/Type1/BaseFont/Times-Roman>>endobj\n");
    pdf.append(
        "5 0 obj<</Length 44>>stream\nBT /F1 12 Tf 100 700 Td "
            + "(Test PDF) Tj ET\nendstream\nendobj\n");
    pdf.append(
        "xref\n0 6\n0000000000 65535 f \n0000000010 00000 n \n0000000053 00000 n \n"
            + "0000000125 00000 n \n0000000284 00000 n \n0000000354 00000 n \n");
    pdf.append("trailer<</Size 6/Root 1 0 R>>\nstartxref\n408\n%%EOF");
    return pdf.toString().getBytes();
  }

  private byte[] loadTestFile(String filename) throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/test-documents/" + filename)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Test file not found: " + filename);
      }
      return inputStream.readAllBytes();
    }
  }

  private byte[] createMinimalDocxBytes() {
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

  private byte[] createMinimalDocBytes() {
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
}
