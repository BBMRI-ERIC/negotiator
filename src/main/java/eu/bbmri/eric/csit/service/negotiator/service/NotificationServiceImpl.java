package eu.bbmri.eric.csit.service.negotiator.service;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@CommonsLog
@Service
public class NotificationServiceImpl implements NotificationService{

    @Autowired
    private JavaMailSender emailSender;
    @Override
    public boolean sendEmailToResourceRepresentatives(String resourceId, String mailBody) {
        return true;
    }
}
