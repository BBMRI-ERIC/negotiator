package eu.bbmri.eric.csit.service.negotiator.dto.person;

import java.util.List;

public class UserInfoModel extends UserResponseModel {
  public UserInfoModel(UserResponseModel userResponseModel, List<String> roles) {
    super(
        userResponseModel.getId(),
        userResponseModel.getSubjectId(),
        userResponseModel.getName(),
        userResponseModel.getEmail(),
        userResponseModel.isRepresentativeOfAnyResource());
    this.roles = roles;
  }

  private List<String> roles;

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }
}
