package eu.bbmri_eric.negotiator.shared;

import jakarta.validation.groups.Default;

public interface ValidationGroups {

  interface Create extends Default {}

  interface Update extends Default {}
}
