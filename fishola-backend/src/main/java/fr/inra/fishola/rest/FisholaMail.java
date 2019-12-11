package fr.inra.fishola.rest;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;

@Value.Immutable
public interface FisholaMail {

    String getFrom();

    ImmutableSet<String> getTos();

    String getSubject();

    String getBody();

    @Value.Check
    default void check() {
        Preconditions.checkArgument(!getTos().isEmpty(), "Il faut au moins un destinataire");
        for (String to : getTos()) {
            Preconditions.checkArgument(StringUtils.isNotBlank(to), "Il ne faut pas de destinataire vide");
        }
        Preconditions.checkArgument(StringUtils.isNotBlank(getSubject()), "Il faut fournir un sujet");
    }

}
