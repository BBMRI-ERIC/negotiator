package eu.bbmri_eric.negotiator.email;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
class NotificationEmailServiceImpl implements NotificationEmailService {

  private final NotificationEmailRepository notificationEmailRepository;
  private final ModelMapper modelMapper;

  public NotificationEmailServiceImpl(
      NotificationEmailRepository notificationEmailRepository, ModelMapper modelMapper) {
    this.notificationEmailRepository = notificationEmailRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  public Page<NotificationEmailDTO> findAllWithFilters(NotificationEmailFilterDTO filters) {
    Pageable pageable = createPageable(filters.getPage(), filters.getSize(), filters.getSort());

    LocalDateTime sentAfter = filters.getSentAfter();
    LocalDateTime sentBefore = filters.getSentBefore();

    Specification<NotificationEmail> spec =
        NotificationEmailSpecification.withFilters(filters.getAddress(), sentAfter, sentBefore);

    Page<NotificationEmail> emailPage = notificationEmailRepository.findAll(spec, pageable);

    return emailPage.map(email -> modelMapper.map(email, NotificationEmailDTO.class));
  }

  @Override
  public NotificationEmailDTO findById(Long id) {
    NotificationEmail email =
        notificationEmailRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new EntityNotFoundException("Notification email with id " + id + " not found"));

    return modelMapper.map(email, NotificationEmailDTO.class);
  }

  private Pageable createPageable(int page, int size, String sort) {
    if (sort == null || sort.isEmpty()) {
      return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
    }

    String[] sortParts = sort.split(",");
    String property = sortParts[0];
    Sort.Direction direction =
        sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

    return PageRequest.of(page, size, Sort.by(direction, property));
  }

  private LocalDateTime parseDateTime(String dateTimeString) {
    if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
      return null;
    }
    try {
      return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } catch (Exception e) {
      log.warn("Failed to parse date time: " + dateTimeString + ". Using null instead.");
      return null;
    }
  }
}
