package fr.inrae.fishola.database;

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

import fr.inrae.fishola.entities.Tables;
import fr.inrae.fishola.entities.tables.daos.DocumentationDao;
import fr.inrae.fishola.entities.tables.daos.EditorialDao;
import fr.inrae.fishola.entities.tables.pojos.Documentation;
import fr.inrae.fishola.entities.tables.pojos.Editorial;
import java.util.Set;
import org.jooq.Record2;
import org.jooq.Result;

import javax.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.impl.DAOImpl;

@Singleton
public class EditorialAndDocumentationDao extends AbstractFisholaDao {

    public List<Documentation> listDocumentationsWithoutContent() {
        List<Documentation> result = withContext(context -> context.selectFrom(Tables.DOCUMENTATION)
                .where(Tables.DOCUMENTATION.CONTENT.isNull())
                .fetchInto(Documentation.class));
        return result;
    }

    public LinkedHashMap<UUID, String> listDocumentations() {
        return withContext(context -> {
            Result<Record2<UUID, String>> tuples = context.select(Tables.DOCUMENTATION.ID, Tables.DOCUMENTATION.NAME)
                    .from(Tables.DOCUMENTATION)
                    .fetch();

            LinkedHashMap<UUID, String> result = new LinkedHashMap<>();
            tuples.forEach(record -> result.put(record.value1(), record.value2()));
            return result;
        });
    }

    public Optional<Documentation> getDocumentation(UUID docId) {
        Documentation doc = withDao(DocumentationDao.class, dao -> dao.findById(docId));
        Optional<Documentation> result = Optional.ofNullable(doc);
        return result;
    }

    public List<Editorial> getEditorials() {
        List<Editorial> editorials = withDao(EditorialDao.class, DAOImpl::findAll);
        return editorials;
    }

    public void updateEditorial(Editorial editorial) {
        withDaoNoResult(EditorialDao.class, dao -> dao.update(editorial));
    }

    public Optional<Editorial> findEditorial(String name) {
        Optional<Editorial> result = withDao(EditorialDao.class, dao -> {
            List<Editorial> editorials = dao.fetchByName(name);
            if (editorials.size() == 1) {
                return Optional.of(editorials.iterator().next());
            }
            return Optional.empty();
        });
        return result;
    }

    public void updateDocumentation(Documentation documentation) {
        withDaoNoResult(DocumentationDao.class, dao -> dao.update(documentation));
    }

}
