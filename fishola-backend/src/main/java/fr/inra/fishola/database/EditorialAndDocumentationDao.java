package fr.inra.fishola.database;

import fr.inra.fishola.entities.Tables;
import fr.inra.fishola.entities.tables.daos.DocumentationDao;
import fr.inra.fishola.entities.tables.daos.EditorialDao;
import fr.inra.fishola.entities.tables.pojos.Documentation;
import fr.inra.fishola.entities.tables.pojos.Editorial;
import org.jooq.Record2;
import org.jooq.Result;

import javax.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class EditorialAndDocumentationDao extends AbstractFisholaDao {

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

    public Optional<byte[]> getDocumentation(UUID docId) {
        Documentation doc = withDao(DocumentationDao.class, dao -> dao.findById(docId));
        Optional<byte[]> result = Optional.ofNullable(doc).map(Documentation::getContent);
        return result;
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

}
