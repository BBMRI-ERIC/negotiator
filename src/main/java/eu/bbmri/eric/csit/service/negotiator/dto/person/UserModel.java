package eu.bbmri.eric.csit.service.negotiator.dto.person;

import lombok.Getter;
import lombok.Setter;

/** A DTO model for a person authenticated via OAuth. */
@Getter
@Setter
public class UserModel {
  private String id;
  private String subjectId;
  private String name;
  private String email;

  public void setId(Long id) {
    this.id = String.valueOf(id);
  }
}
