package eu.bbmri_eric.negotiator.user;

import java.util.List;

public class UserInfoModel extends UserResponseModel {
  public UserInfoModel(UserResponseModel userResponseModel, List<String> roles) {
    super(
        userResponseModel.getId(),
        userResponseModel.getSubjectId(),
        userResponseModel.getName(),
        userResponseModel.getEmail(),
        userResponseModel.isRepresentativeOfAnyResource(),
        userResponseModel.isAdmin(),
        userResponseModel.isNetworkManager(),
        userResponseModel.getLastLogin(),
        userResponseModel.isServiceAccount());
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
