package fr.inra.fishola.rest.feedback;

import java.util.Optional;

public class FeedbackBean {

    public String categoryId;
    public Optional<String> email = Optional.empty();
    public String description;
    public boolean withPicture;
    public Optional<String> picture = Optional.empty();

    @Override
    public String toString() {
        return "FeedbackBean{" +
                "categoryId='" + categoryId + '\'' +
                ", email=" + email +
                ", description='" + description + '\'' +
                ", withPicture=" + withPicture +
                '}';
    }
}
