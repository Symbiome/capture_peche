package fr.inrae.fishola.mails;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;

import java.time.LocalDateTime;
import java.util.Optional;

@Value.Immutable()
@Value.Style(
        // Le builder aura une méthode "fromInstance" qui permet de l'initialiser à partir d'une autre instance. La
        // méthode "from" est masquée par FisholaMail#from donc on choisit d'introduire un nouveau nom par défaut
        from = "fromInstance")
public interface FisholaMail {

    Optional<LocalDateTime> pendingSince();

    String getFrom();

    ImmutableSet<String> getTos();

    String getSubject();

    String getBody();

    @Value.Default
    default ImmutableSet<FisholaMailAttachment> getAttachments() {
        return ImmutableSet.of();
    }

    @Value.Check
    default void check() {
        Preconditions.checkArgument(!getTos().isEmpty(), "Il faut au moins un destinataire");
        for (String to : getTos()) {
            Preconditions.checkArgument(StringUtils.isNotBlank(to), "Il ne faut pas de destinataire vide");
        }
        Preconditions.checkArgument(StringUtils.isNotBlank(getSubject()), "Il faut fournir un sujet");
    }

}
