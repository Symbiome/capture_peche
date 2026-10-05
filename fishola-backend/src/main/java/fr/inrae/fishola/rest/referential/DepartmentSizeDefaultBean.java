package fr.inrae.fishola.rest.referential;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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

import fr.inrae.fishola.entities.tables.pojos.DepartmentAuthorizedSample;

import java.util.UUID;

/**
 * Maillage et taille maximale par défaut d'une espèce sur un département (#246),
 * échangés avec le back-office « Maillages et tailles maximales ».
 */
public class DepartmentSizeDefaultBean {
    public UUID speciesId;
    /** Taille maximale en cm, obligatoire. */
    public Integer maxSize;
    /** Maillage en cm, facultatif (null ou 0 : non défini). */
    public Integer meshSize;

    public static DepartmentSizeDefaultBean of(DepartmentAuthorizedSample sample) {
        DepartmentSizeDefaultBean bean = new DepartmentSizeDefaultBean();
        bean.speciesId = sample.getSpeciesId();
        bean.maxSize = sample.getMaxSize();
        bean.meshSize = sample.getMeshSize();
        return bean;
    }

    public DepartmentAuthorizedSample toEntity(String department) {
        DepartmentAuthorizedSample sample = new DepartmentAuthorizedSample();
        sample.setDepartment(department);
        sample.setSpeciesId(speciesId);
        sample.setMaxSize(maxSize);
        sample.setMeshSize(meshSize == null || meshSize == 0 ? null : meshSize);
        return sample;
    }

    /** Message d'erreur de saisie, ou null si la valeur est valide. */
    public String validationError() {
        if (speciesId == null) {
            return "Espèce manquante";
        }
        if (maxSize == null || maxSize <= 0) {
            return "La taille maximale est obligatoire";
        }
        if (meshSize != null && meshSize < 0) {
            return "Le maillage ne peut pas être négatif";
        }
        return null;
    }
}
