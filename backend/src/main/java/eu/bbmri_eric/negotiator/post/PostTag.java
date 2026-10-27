package eu.bbmri_eric.negotiator.post;

import lombok.Getter;

@Getter
public enum PostTag {
  AUTHOR("author", "Author", "Author of this Request"),
  REPRESENTATIVE("representative", "Representative", "Representative of a resource"),
  ADMIN("admin", "Admin", "Negotiator Administrator"),
  NETWORK_MANAGER(
      "network-manager",
      "NetworkManager",
      "Manager of a Network responsible for moderating requests");

  private final String key;
  private final String name;
  private final String description;

  PostTag(String key, String name, String description) {
    this.key = key;
    this.name = name;
    this.description = description;
  }

  public PostTagDTO toDTO() {
    return new PostTagDTO(key, name, description);
  }
}
