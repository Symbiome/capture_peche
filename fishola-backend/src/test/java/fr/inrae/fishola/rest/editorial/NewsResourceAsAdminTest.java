package fr.inrae.fishola.rest.editorial;

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

import com.google.common.collect.Sets;
import fr.inrae.fishola.database.NewsFisholaDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.entities.tables.daos.LakeDao;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import fr.inrae.fishola.entities.tables.pojos.News;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import io.quarkus.test.junit.QuarkusTest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
class NewsResourceAsAdminTest extends AbstractFisholaTest {

    @Inject
    protected NewsFisholaDao newsDao;

    @Inject
    protected ReferentialDao referentialDao;

    private String token;

    @BeforeEach
    @Transactional
    void loginAsAdminAndCreateRandomNews() {
        token = loginAsAdmin();

        // Insert a non published news
        LocalDateTime now = LocalDateTime.now();
        News unpublishedNews = new News();
        unpublishedNews.setContent("Content");
        unpublishedNews.setName(this.token);
        unpublishedNews.setDatePublicationDebut(now.plusDays(10));
        unpublishedNews.setDatePublicationFin(now.plusDays(20));
        this.newsDao.insert(unpublishedNews, referentialDao.listLakes().stream().map(Lake::getId).collect(Collectors.toSet()));

        // Insert a published news
        News publishedNews = new News();
        publishedNews.setContent("Content");
        publishedNews.setName("published-" + token);
        publishedNews.setDatePublicationDebut(now.minusDays(1));
        publishedNews.setDatePublicationFin(now.plusDays(10));
        this.newsDao.insert(publishedNews, referentialDao.listLakes().stream().map(Lake::getId).collect(Collectors.toSet()));
    }

    @Test
    @Transactional
    void testGetAllNews() {
        // All news should be displayed no matter the publication date
        int expectedNewsCount = this.newsDao.getNews(false, Optional.empty()).size();
        Assertions.assertNotEquals(0, expectedNewsCount);
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/news-all")
                .then()
                .statusCode(200)
                .body("size()", equalTo(expectedNewsCount));
    }

    @Test
    @Transactional
    void testNewsUpdate() {
        // Update news's publication date
        News newsFromDB = newsDao.getNews(false, Optional.empty()).get(0);
        String modifiedName = "modified";
        NewsBean news = new NewsBean(newsFromDB.getId(), newsFromDB.getName(), newsFromDB.getContent(), newsFromDB.getDatePublicationDebut(), newsFromDB.getDatePublicationFin(), newsFromDB.getDateNotificationSent(), newsFromDB.getMiniatureId(), newsFromDB.getIsNational(), Sets.newLinkedHashSet());
        news.name = modifiedName;
        LocalDateTime now = LocalDateTime.now();
        news.isNational = true;
        news.datePublicationDebut = now.minusDays(1);
        news.datePublicationFin = now.plusDays(20);
        news.lakeIds = referentialDao.listLakes().stream().map(Lake::getId).collect(Collectors.toSet());
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(news)
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .put("/api/v1/news-all/" + news.id)
                .then()
                .statusCode(204);
        News updated = newsDao.findById(news.id);
        Assertions.assertEquals(modifiedName, updated.getName());

        // TODO #11679 notifications should be sent due to modified date

        newsDao.deleteById(news.id);
    }

    @Test
    @Transactional
    void testNewsPost() {
        int newsCountBefore = this.newsDao.getNews(false, Optional.empty()).size();
        LocalDateTime now = LocalDateTime.now();
        Set<UUID>  lakeIds = referentialDao.listLakes().stream().map(Lake::getId).collect(Collectors.toSet());
        NewsBean news = new NewsBean(UUID.randomUUID(),"published-newnews", "content", now.minusDays(1), now.plusDays(20), now, UUID.randomUUID(), false, lakeIds);
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(news)
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .post("/api/v1/news-all")
                .then()
                .statusCode(204);

        int newsCountAfter = this.newsDao.getNews(false, Optional.empty()).size();

        // TODO #11679 notifications should be sent due to modified date
        Assertions.assertEquals(newsCountAfter, newsCountBefore + 1);
    }

    @Test
    @Transactional
    void testNewsDelete() {
        int newsCountBefore = this.newsDao.getNews(false, Optional.empty()).size();

        News news = newsDao.getNews(false, Optional.empty()).get(0);
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .delete("/api/v1/news-all/" + news.getId())
                .then()
                .statusCode(204);

        int newsCountAfter = this.newsDao.getNews(false, Optional.empty()).size();

        Assertions.assertEquals(newsCountAfter, newsCountBefore - 1);

    }

}
