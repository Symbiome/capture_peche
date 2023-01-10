package fr.inrae.fishola.rest.referential;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.entities.tables.pojos.Species;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableSpeciesWithAlias.class)
public interface SpeciesWithAlias {

    // Informations liées uniquement à l'espèce
    UUID id();
    String  name();
    boolean builtIn();
    boolean mandatorySize();

    // Informations dépendante du lac
    Optional<String> alias();
    boolean authorizedSample();
    Integer minSize();

    static SpeciesWithAlias of(Species source) {
        SpeciesWithAlias result = of(source, Optional.empty(), false, 0);
        return result;
    }

    static SpeciesWithAlias of(Species source, Optional<String> alias, boolean authorizedSample, Integer minSize) {
        ImmutableSpeciesWithAlias result = ImmutableSpeciesWithAlias.builder()
                .id(source.getId())
                .name(source.getName())
                .builtIn(source.getBuiltIn())
                .mandatorySize(source.getMandatorySize())
                .alias(alias)
                .minSize(minSize)
                .authorizedSample(authorizedSample)
                .build();
        return result;
    }

}
