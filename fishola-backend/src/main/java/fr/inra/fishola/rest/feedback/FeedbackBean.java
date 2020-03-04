package fr.inra.fishola.rest.feedback;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Date;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableFeedbackBean.class)
public interface FeedbackBean {

    String category();

    Optional<String> userId();

    Optional<String> email();

    Optional<String> description();

    @Value.Auxiliary // exclu des equals/hashCode/toString
    Optional<String> screenshot();

    Optional<String> browser();

    Optional<String> os();

    Optional<String> platform();

    Optional<String> screenResolution();

    Optional<String> displaySize();

    Optional<String> locale();

    Optional<String> version();

    Optional<Date> date();

    Optional<String> location();

    Optional<String> locationTitle();

}
