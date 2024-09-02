package eu.bbmri_eric.negotiator.governance.resource.dto;

/** Generic interface for Resources * */
public interface GenericResource {
  /**
   * Gets the unique identifier of the Resource
   *
   * @return The unique Id of the Resource
   */
  String getId();

  /**
   * Gets the name of the Resource
   *
   * @return The name of the Resource
   */
  String getName();

  /**
   * Gets the description of the Resource
   *
   * @return The description of the Resource
   */
  String getDescription();

  /**
   * Gets the Organization of the Resource
   *
   * @return The Organization the Resource belongs to
   */
  GenericOrganization getOrganization();
}
