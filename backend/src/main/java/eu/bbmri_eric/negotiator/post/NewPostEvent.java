package eu.bbmri_eric.negotiator.post;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NewPostEvent extends ApplicationEvent {
    private String negotiationId;
    private String postId;
    private Long organizationId;

    public NewPostEvent(Object source) {
        super(source);
    }

    public NewPostEvent(Object source, String negotiationId, String postId) {
        super(source);
        this.negotiationId = negotiationId;
        this.postId = postId;
    }

    public NewPostEvent(Object source, String negotiationId, String postId, Long organizationId) {
        super(source);
        this.negotiationId = negotiationId;
        this.postId = postId;
        this.organizationId = organizationId;
    }
}
