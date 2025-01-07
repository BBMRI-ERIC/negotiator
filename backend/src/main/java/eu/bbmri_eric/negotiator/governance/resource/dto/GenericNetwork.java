package eu.bbmri_eric.negotiator.governance.resource.dto;

/** Generic interface for Networks * */
public interface GenericNetwork {

  /**
   * Gets the unique identifier of the Network
   *
   * @return The unique Id of the Network
   */
  String getId();

  /**
   * Sets the unique identifier of the Network
   *
   * @param id The unique Id assigned to the Network
   */
  void setId(String id);

  /**
   * Gets the name of the Network
   *
   * @return The name of the network
   */
  String getName();

  /**
   * Sets the name of the Network
   *
   * @param name The name assigned to the Network
   */
  void setName(String name);

  /**
   * Gets the contact email of the Network's representative
   *
   * @return The name of the network
   */
  String getContactEmail();

  /**
   * Sets the contact email of the Network's representative
   *
   * @param contactEmail The contact email of the Network's representative
   */
  void setContactEmail(String contactEmail);

  /**
   * Gets the URI of the Network
   *
   * @return The URI of the network
   */
  String getUri();

  /**
   * Sets the URI of the Network
   *
   * @param uri The URI assigned to the Network
   */
  void setUri(String uri);
}
