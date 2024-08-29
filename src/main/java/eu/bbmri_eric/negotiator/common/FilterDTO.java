package eu.bbmri_eric.negotiator.common;

/** Represents a contract for a filter DTO. */
public interface FilterDTO {
  int getPage();

  int getSize();

  void setPage(int page);

  void setSize(int size);
}
