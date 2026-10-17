package fr.inrae.fishola.rest.imports.survey;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Archive ZIP téléchargeable des gabarits de l'import « enquête terrain » : les deux
 * formats acceptés (classeur XLSX multi-feuilles {@link SurveyTemplate}, CSV plat
 * {@link SurveyFlatTemplate}), un exemple complet au format CSV plat et un LISEZ-MOI
 * qui présente les deux options. Un seul téléchargement : l'utilisateur découvre
 * toujours qu'il a le choix entre les deux formats.
 */
public final class SurveyTemplateBundle {

    private SurveyTemplateBundle() {}

    public static final String FILE_NAME = "gabarits-enquete-terrain.zip";
    public static final String README_NAME = "LISEZ-MOI.txt";
    public static final String EXAMPLE_NAME = "exemple-enquete-terrain.csv";

    private static final String RESOURCE_DIRECTORY = "/imports/survey/";

    /** Archive ZIP : LISEZ-MOI, gabarit XLSX, gabarit CSV plat et exemple CSV plat. */
    public static byte[] build() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            addEntry(zip, README_NAME, readResource(README_NAME));
            addEntry(zip, SurveyTemplate.FILE_NAME, SurveyTemplate.build());
            addEntry(zip, SurveyFlatTemplate.FILE_NAME, SurveyFlatTemplate.build());
            addEntry(zip, EXAMPLE_NAME, readResource(EXAMPLE_NAME));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static void addEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    /** Fichier versionné dans {@code src/main/resources/imports/survey/}. */
    static byte[] readResource(String name) throws IOException {
        try (InputStream in = SurveyTemplateBundle.class.getResourceAsStream(RESOURCE_DIRECTORY + name)) {
            if (in == null) {
                throw new IOException("Ressource introuvable : " + RESOURCE_DIRECTORY + name);
            }
            return in.readAllBytes();
        }
    }
}
