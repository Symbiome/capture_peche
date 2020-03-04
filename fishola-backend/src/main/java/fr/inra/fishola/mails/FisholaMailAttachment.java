package fr.inra.fishola.mails;

import com.google.common.net.MediaType;
import org.immutables.value.Value;

@Value.Immutable
public interface FisholaMailAttachment {

    @Value.Auxiliary // exclu des equals/hashCode/toString
    byte[] getBytes();

    String getName();

    MediaType getType();

}
