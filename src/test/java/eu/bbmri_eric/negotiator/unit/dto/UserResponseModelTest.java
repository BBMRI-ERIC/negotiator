package eu.bbmri_eric.negotiator.unit.dto;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.negotiator.user.UserResponseModel;
import org.junit.jupiter.api.Test;

public class UserResponseModelTest {

  @Test
  public void testGetterAndSetter() {
    UserResponseModel userResponseModel = new UserResponseModel();
    userResponseModel.setId(Long.valueOf("12345"));
    userResponseModel.setSubjectId("abc");
    userResponseModel.setName("John Doe");
    userResponseModel.setEmail("johndoe@example.com");
    userResponseModel.setRepresentativeOfAnyResource(true);

    assertEquals("12345", userResponseModel.getId());
    assertEquals("abc", userResponseModel.getSubjectId());
    assertEquals("John Doe", userResponseModel.getName());
    assertEquals("johndoe@example.com", userResponseModel.getEmail());
    assertTrue(userResponseModel.isRepresentativeOfAnyResource());
  }

  @Test
  public void testEqualsAndHashCode() {
    UserResponseModel user1 = new UserResponseModel();
    user1.setId(Long.valueOf("12345"));
    user1.setSubjectId("abc");
    user1.setName("John Doe");
    user1.setEmail("johndoe@example.com");

    UserResponseModel user2 = new UserResponseModel();
    user2.setId(Long.valueOf("12345"));
    user2.setSubjectId("abc");
    user2.setName("John Doe");
    user2.setEmail("johndoe@example.com");

    assertTrue(user1.equals(user2) && user2.equals(user1));
    assertEquals(user1.hashCode(), user2.hashCode());
  }
}
