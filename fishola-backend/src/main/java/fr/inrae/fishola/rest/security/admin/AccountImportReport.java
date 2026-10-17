package fr.inrae.fishola.rest.security.admin;

/*-
 * #%L
 * Carnet de pêche :: Backend
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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

import java.util.ArrayList;
import java.util.List;

/**
 * Rapport d'un import de comptes (#192) : une entrée par ligne lue, ou une
 * erreur de fichier (colonnes manquantes) si rien n'a pu être lu.
 */
public class AccountImportReport {

    public int total;
    public int created;
    public int ignored;
    public int rejected;
    public String fileError;
    public List<AccountImportLine> lines = new ArrayList<>();

    /**
     * Rapport d'un fichier illisible : aucune ligne traitée.
     *
     * @param message cause, affichée telle quelle dans le back-office
     * @return le rapport
     */
    public static AccountImportReport fileError(String message) {
        AccountImportReport report = new AccountImportReport();
        report.fileError = message;
        return report;
    }

    /**
     * Ajoute le résultat d'une ligne et met à jour les totaux.
     *
     * @param result ligne traitée
     */
    public void add(AccountImportLine result) {
        lines.add(result);
        total++;
        switch (result.status) {
            case AccountImportLine.CREATED -> created++;
            case AccountImportLine.IGNORED -> ignored++;
            default -> rejected++;
        }
    }
}
