package eu.bbmri.eric.csit.service.negotiator.service;

import org.springframework.stereotype.Service;

@Service
public interface NotificationService {
    /**
     * Sends an email notification to Resource Representatives.
     * @param resourceId of which representatives you want to contact.
     * @param mailBody content of the email message.
     * @return true if email was successfully sent out.
     */
    boolean sendEmailToResourceRepresentatives(String resourceId, String mailBody);
}
