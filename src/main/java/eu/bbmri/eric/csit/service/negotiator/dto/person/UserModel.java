package eu.bbmri.eric.csit.service.negotiator.dto.person;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A DTO model for a person authenticated via OAuth. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserModel {
  private final String type = "user";
  private String id;
  private String subjectId;
  private String name;
  private String email;
  private boolean isRepresentativeOfAnyResource;

  public void setId(Long id) {
    this.id = String.valueOf(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserModel userModel = (UserModel) o;
    return Objects.equals(id, userModel.id)
        && Objects.equals(subjectId, userModel.subjectId)
        && Objects.equals(name, userModel.name)
        && Objects.equals(email, userModel.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, subjectId, name, email);
  }
}
