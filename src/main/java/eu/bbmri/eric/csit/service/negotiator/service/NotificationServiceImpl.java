package eu.bbmri.eric.csit.service.negotiator.service;


import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.regex.Pattern;

@NoArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    @Override
    public boolean sendEmail(String recipientAddress) {
        return validateEmailAddress(recipientAddress);
    }

    private static boolean validateEmailAddress(String recipientAddress) {
        if (Objects.isNull(recipientAddress)){
            return false;
        }
        String regexPattern = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";
        return Pattern.compile(regexPattern).matcher(recipientAddress).matches();
    }
}
