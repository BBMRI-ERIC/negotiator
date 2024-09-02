package eu.bbmri_eric.negotiator.governance.resource.dto;

/** Generic interface for Organizations * */
public interface GenericOrganization {
  /**
   * Gets the unique identifier of the Organization
   *
   * @return The unique Id of the Organization
   */
  String getId();

  /**
   * Gets the name of the Organization
   *
   * @return The name of the Organization
   */
  String getName();
}
