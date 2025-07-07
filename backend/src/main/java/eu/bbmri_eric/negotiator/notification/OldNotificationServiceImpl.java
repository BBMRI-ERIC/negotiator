package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.email.EmailService;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@CommonsLog
@Transactional
public class OldNotificationServiceImpl implements OldNotificationService {

    NotificationRepository notificationRepository;
    PersonRepository personRepository;
    ModelMapper modelMapper;
    EmailService emailService;
    TemplateEngine templateEngine;
    NegotiationRepository negotiationRepository;

    public OldNotificationServiceImpl(
            NotificationRepository notificationRepository,
            PersonRepository personRepository,
            ModelMapper modelMapper,
            EmailService emailService,
            TemplateEngine templateEngine,
            NegotiationRepository negotiationRepository) {
        this.notificationRepository = notificationRepository;
        this.personRepository = personRepository;
        this.modelMapper = modelMapper;
        this.emailService = emailService;
        this.templateEngine = templateEngine;
        this.negotiationRepository = negotiationRepository;
    }


    private static Set<Person> getRepresentativesForNegotiation(Negotiation negotiation) {
        return negotiation.getResources().stream()
                .filter(
                        resource ->
                                Objects.equals(
                                        negotiation.getCurrentStateForResource(resource.getSourceId()),
                                        NegotiationResourceState.SUBMITTED))
                .map(Resource::getRepresentatives)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @NonNull
    private static Set<Person> getRepresentativesOfOrganization(Post post) {
        Set<Person> representatives = new HashSet<>(Set.of());
        post.getOrganization()
                .getResources()
                .forEach(resource -> representatives.addAll(resource.getRepresentatives()));
        return representatives;
    }

    @Override
    @Transactional
    public void notifyUsersAboutNewPost(Post post) {
        log.info("Notifying users about new post.");
        if (post.isPublic()
                && Objects.equals(post.getNegotiation().getCurrentState(), NegotiationState.IN_PROGRESS)) {
            createNotificationsForRepresentatives(post);
        } else if (!post.isPublic() && Objects.nonNull(post.getOrganization())) {
            createNotificationsForPrivatePost(post);
        }
    }

    private void createNotificationsForPrivatePost(Post post) {
        Set<Person> representatives = getRepresentativesOfOrganization(post);
        for (Person representative : representatives) {
            if (!representative.getId().equals(post.getCreatedBy().getId())) {
                notificationRepository.save(
                        Notification.builder()
                                .negotiationId(post.getNegotiation().getId())
                                .recipientId(representative.getId())
                                .message(
                                        "Negotiation %s had a new post by %s"
                                                .formatted(post.getNegotiation().getId(), post.getCreatedBy().getName()))
                                .build());
            }
        }
    }

    private void createNotificationsForRepresentatives(Post post) {
        for (Person representative : getRepresentativesForNegotiation(post.getNegotiation())) {
            if (!representative.getId().equals(post.getCreatedBy().getId())) {
                notificationRepository.save(
                        Notification.builder()
                                .negotiationId(post.getNegotiation().getId())
                                .recipientId(representative.getId())
                                .message(
                                        "Negotiation %s had a new post by %s"
                                                .formatted(post.getNegotiation().getId(), post.getCreatedBy().getName()))
                                .build());
            }
        }
    }


}
