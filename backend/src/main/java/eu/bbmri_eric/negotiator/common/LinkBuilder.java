package eu.bbmri_eric.negotiator.common;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@CommonsLog
public class LinkBuilder {
  /**
   * Get page links for HATEOAS response models
   *
   * @param baseUri base uri of the request, e.g. /api/resources
   * @param filterDTO filter DTO containing the filter parameters
   * @param pageMetadata page metadata
   * @return list of page links
   */
  public static @NonNull List<Link> getPageLinks(
      URI baseUri, FilterDTO filterDTO, PagedModel.PageMetadata pageMetadata) {
    List<Link> links = new ArrayList<>();

    links.add(Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.CURRENT));
    final int currentPage = filterDTO.getPage();
    if (pageMetadata.getNumber() > 0) {
      filterDTO.setPage(0);
      links.add(Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.FIRST));
      filterDTO.setPage(currentPage - 1);
      links.add(
          Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.PREVIOUS));
    }

    if (pageMetadata.getNumber() < pageMetadata.getTotalPages() - 1) {
      filterDTO.setPage(currentPage + 1);
      links.add(Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.NEXT));
      filterDTO.setPage((int) pageMetadata.getTotalPages() - 1);
      links.add(Link.of(createBaseUriBuilder(baseUri, filterDTO)).withRel(IanaLinkRelations.LAST));
    }

    return links;
  }

  private static String createBaseUriBuilder(URI baseUri, FilterDTO filterDTO) {
    return UriComponentsBuilder.fromUri(baseUri)
        .queryParams(getQueryParams(filterDTO))
        .build()
        .toString();
  }

  private static MultiValueMap<String, String> getQueryParams(Object filterDTO) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    Field[] fields = filterDTO.getClass().getDeclaredFields();

    for (Field field : fields) {
      try {
        field.setAccessible(true);
        Object value = field.get(filterDTO);
        if (value != null) {
          queryParams.add(field.getName(), String.valueOf(value));
        }
      } catch (Exception e) {
        log.error("Error while getting query params", e);
      }
    }

    return queryParams;
  }
}
