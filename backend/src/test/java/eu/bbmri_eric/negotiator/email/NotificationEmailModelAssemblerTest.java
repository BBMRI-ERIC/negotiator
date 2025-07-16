package eu.bbmri_eric.negotiator.email;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

class NotificationEmailModelAssemblerTest {

  private NotificationEmailModelAssembler assembler;

  @BeforeEach
  void setUp() {
    assembler = new NotificationEmailModelAssembler();
  }

  @Test
  void toModel_CreatesEntityModelWithSelfAndEmailsLinks() {
    NotificationEmailDTO dto =
        NotificationEmailDTO.builder()
            .id(1L)
            .address("test@example.com")
            .message("Test message")
            .sentAt(LocalDateTime.now())
            .build();

    EntityModel<NotificationEmailDTO> model = assembler.toModel(dto);

    assertNotNull(model);
    assertEquals(dto, model.getContent());
    assertTrue(model.hasLink("self"));
    assertTrue(model.hasLink("emails"));
    assertTrue(model.getLink("self").isPresent());
    String selfLink = model.getLink("self").get().getHref();
    assertTrue(selfLink.contains("1"), "Self link should contain the email ID");
  }

  @Test
  void toPagedModel_CreatesPagedModelWithCorrectMetadataAndLinks() {
    NotificationEmailDTO dto1 =
        NotificationEmailDTO.builder()
            .id(1L)
            .address("test1@example.com")
            .message("Test message 1")
            .sentAt(LocalDateTime.now())
            .build();

    NotificationEmailDTO dto2 =
        NotificationEmailDTO.builder()
            .id(2L)
            .address("test2@example.com")
            .message("Test message 2")
            .sentAt(LocalDateTime.now())
            .build();

    List<NotificationEmailDTO> content = List.of(dto1, dto2);
    Page<NotificationEmailDTO> page = new PageImpl<>(content, PageRequest.of(0, 10), 2);
    NotificationEmailFilterDTO filters = new NotificationEmailFilterDTO();

    PagedModel<EntityModel<NotificationEmailDTO>> pagedModel =
        assembler.toPagedModel(page, filters);

    assertNotNull(pagedModel);
    assertEquals(2, pagedModel.getContent().size());

    PagedModel.PageMetadata metadata = pagedModel.getMetadata();
    assertNotNull(metadata);
    assertEquals(10, metadata.getSize());
    assertEquals(0, metadata.getNumber());
    assertEquals(2, metadata.getTotalElements());
    assertEquals(1, metadata.getTotalPages());

    // Verify each entity model has proper links
    pagedModel
        .getContent()
        .forEach(
            entityModel -> {
              assertTrue(entityModel.hasLink("self"));
              assertTrue(entityModel.hasLink("emails"));
            });
  }

  @Test
  void toPagedModel_WithEmptyPage_CreatesPagedModelWithNoContent() {
    Page<NotificationEmailDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    NotificationEmailFilterDTO filters = new NotificationEmailFilterDTO();

    PagedModel<EntityModel<NotificationEmailDTO>> pagedModel =
        assembler.toPagedModel(emptyPage, filters);

    assertNotNull(pagedModel);
    assertTrue(pagedModel.getContent().isEmpty());

    PagedModel.PageMetadata metadata = pagedModel.getMetadata();
    assertNotNull(metadata);
    assertEquals(0, metadata.getTotalElements());
  }
}
