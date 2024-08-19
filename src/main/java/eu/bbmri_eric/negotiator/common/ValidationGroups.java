package eu.bbmri_eric.negotiator.common;

import jakarta.validation.groups.Default;

public interface ValidationGroups {

  interface Create extends Default {}

  interface Update extends Default {}
}
