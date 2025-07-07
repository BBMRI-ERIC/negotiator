package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.post.Post;

public interface OldNotificationService {
    /**
     * Create notifications for all relevant Users about a new Post.
     *
     * @param post that was created.
     */
    void notifyUsersAboutNewPost(Post post);
}
