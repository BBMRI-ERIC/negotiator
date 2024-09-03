package eu.bbmri_eric.negotiator.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

/** A DTO model for a person authenticated via OAuth. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Relation(collectionRelation = "users", itemRelation = "user")
public class UserResponseModel {
  private String id;
  private String subjectId;
  private String name;
  private String email;
  @JsonIgnore private boolean isRepresentativeOfAnyResource;
  @JsonIgnore private boolean isNetworkManager = false;

  public void setId(Long id) {
    this.id = String.valueOf(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserResponseModel userModel = (UserResponseModel) o;
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
