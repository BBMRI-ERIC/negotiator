package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri.eric.csit.service.negotiator.dto.person.ResourceModel;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserModel;
import eu.bbmri.eric.csit.service.negotiator.mappers.UserModelAssembler;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;

public class UserModelAssemblerTest {
  private final UserModelAssembler assembler = new UserModelAssembler();

  @Test
  void toModel_null_throwsNullPointer() {
    assertThrows(
        NullPointerException.class,
        () -> {
          assembler.toModel(null);
        });
  }

  @Test
  void toModel_validUserModel_ok() {
    UserModel userModel =
        UserModel.builder()
            .name("Lucifer")
            .id("666")
            .email("lucifer.morningstar@ea.com")
            .subjectId("1@hell.com")
            .representedResources(Set.of(ResourceModel.builder().externalId("777").build()))
            .build();
    EntityModel<UserModel> entityModel = assembler.toModel(userModel);
    assertEquals(userModel, entityModel.getContent());
    assertEquals("/v3/users/666", entityModel.getLink("self").get().getHref());
    assertEquals("/v3/users", entityModel.getLink("users").get().getHref());
  }
}
