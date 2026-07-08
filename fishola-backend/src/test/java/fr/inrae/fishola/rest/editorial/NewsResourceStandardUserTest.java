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

import fr.inrae.fishola.database.NewsFisholaDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.entities.tables.pojos.WaterEntity;
import fr.inrae.fishola.entities.tables.pojos.News;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
class NewsResourceStandardUserTest extends AbstractFisholaTest {

    @Inject
    protected NewsFisholaDao newsDao;

    @Inject
    protected ReferentialDao referentialDao;

    private String token;

    @BeforeEach
    @Transactional
    void loginAndCreateRandomNews() {
        this.token = login("thimel@codelutin.com", "sispea");

        // Insert a non published news
        LocalDateTime now = LocalDateTime.now();
        News unpublishedNews = new News();
        unpublishedNews.setContent("Content");
        unpublishedNews.setName(this.token);
        unpublishedNews.setDatePublicationDebut(now.plusDays(10));
        unpublishedNews.setDatePublicationFin(now.plusDays(20));
        this.newsDao.insert(unpublishedNews, referentialDao.listWaterEntities().stream().map(WaterEntity::getId).collect(Collectors.toSet()));

        // Insert a published news
        News publishedNews = new News();
        publishedNews.setContent("Content");
        publishedNews.setName("published-" + token);
        publishedNews.setDatePublicationDebut(now.minusDays(10));
        publishedNews.setDatePublicationFin(now.plusDays(10));
        this.newsDao.insert(publishedNews, referentialDao.listWaterEntities().stream().map(WaterEntity::getId).collect(Collectors.toSet()));
    }

    @Test
    @Transactional
    void testGetPublishedNews() {
        // Get news : only news with active publication date should be displayed
        int expectedPublishedNewsCount = this.newsDao.getNews(false, Optional.empty()).stream().filter(
                news -> news.getName().startsWith("published-")
        ).toList().size();
        List<News> publishedNews = this.newsDao.getNews(true, Optional.empty());
        Assertions.assertEquals(expectedPublishedNewsCount, publishedNews.size());
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/api/v1/news")
                .then()
                .body("size()", equalTo(expectedPublishedNewsCount));
    }

    @Test
    void testGetAllNewsIsForbidden() {
        // Shouldn't be authorised for non admin
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/news-all")
                .then()
                .statusCode(401);

    }

    @Test
    @Transactional
    void testNewsUpdateIsForbidden() {
        // Shouldn't be authorised for non admin
        News news = newsDao.getNews(false, Optional.empty()).get(0);
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .put("/api/v1/news-all/" + news.getId())
                .then()
                .statusCode(401);
    }
    @Test
    void testNewsPostIsForbidden() {
        // Shouldn't be authorised for non admin
        News news = new News();
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .body(news)
                .post("/api/v1/news-all")
                .then()
                .statusCode(401);
    }

    @Test
    @Transactional
    void testNewsDeleteIsForbidden() {
        // Shouldn't be authorised for non admin
        News news = newsDao.getNews(false,Optional.empty()).get(0);
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, token)
                .delete("/api/v1/news-all/" + news.getId())
                .then()
                .statusCode(401);

    }

}
