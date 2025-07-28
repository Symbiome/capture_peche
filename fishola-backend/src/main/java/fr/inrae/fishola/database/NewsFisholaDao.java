package fr.inrae.fishola.database;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2023 INRAE - UMR CARRTEL
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
import fr.inrae.fishola.entities.Tables;
import fr.inrae.fishola.entities.tables.daos.FisholaAdminLakesDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.entities.tables.daos.NewsDao;
import fr.inrae.fishola.entities.tables.daos.NewsLakeDao;
import fr.inrae.fishola.entities.tables.daos.NewsPictureDao;
import fr.inrae.fishola.entities.tables.daos.NextScheduledCourrielNotificationCheckDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdminLakes;
import fr.inrae.fishola.entities.tables.pojos.News;
import fr.inrae.fishola.entities.tables.pojos.NewsLake;
import fr.inrae.fishola.entities.tables.pojos.NewsPicture;
import fr.inrae.fishola.entities.tables.pojos.NextScheduledCourrielNotificationCheck;
import fr.inrae.fishola.entities.tables.records.NewsRecord;
import fr.inrae.fishola.rest.ImageHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.impl.DAOImpl;

@Singleton
public class NewsFisholaDao extends AbstractFisholaDao {

    public static final UUID TEMPORARY_NEWS_ID = UUID.randomUUID();

    public List<News> getNews(boolean onlyListPublishedNews, Optional<FisholaAdmin> admin) {
        List<News> allNews = withDao(NewsDao.class, DAOImpl::findAll);
        if (onlyListPublishedNews) {
            LocalDateTime now = LocalDateTime.now();
            allNews = allNews.stream().filter(
                    news ->
                            news.getDatePublicationDebut() != null && now.isAfter(news.getDatePublicationDebut()) &&
                            news.getDatePublicationFin() != null && now.isBefore(news.getDatePublicationFin())
            ).toList();
        }
        if (admin.isPresent() && !admin.get().getIsNationalAdmin()) {
            // For local admin, only keep national and lake-related actus
            UUID[] adminLakeIds = withDao(FisholaAdminLakesDao.class, dao -> dao.fetchByFisholaAdminId(admin.get().getId()).stream().map(FisholaAdminLakes::getLakeId)).toArray(UUID[]::new);
            Set<UUID> newsOfReleventLakes =  withDao(NewsLakeDao.class, dao -> dao.fetchByLakeId(adminLakeIds).stream().map(NewsLake::getNewsId).collect(Collectors.toSet()));
            allNews = allNews.stream().filter(news ->
                    newsOfReleventLakes.contains(news.getId())
            ).collect(Collectors.toList());
        }
        allNews = allNews.stream().sorted(
                (n1, n2) -> -1 * n1.getDatePublicationDebut().compareTo(n2.getDatePublicationDebut())
        ).toList();
        return allNews;
    }

    public List<News> getPublishedNewsForLake(UUID lakeId) {
        DSLContext context = newContext();
        LocalDateTime now = LocalDateTime.now();
        List<News> allNews = context
                .select(Tables.NEWS.asterisk())
                .from(Tables.NEWS)
                .leftJoin(Tables.NEWS_LAKE).on(Tables.NEWS_LAKE.NEWS_ID.eq(Tables.NEWS.ID))
                .where(
                    Tables.NEWS.IS_NATIONAL.eq(true).or(
                    Tables.NEWS_LAKE.LAKE_ID.eq(lakeId))
                )
                .and(Tables.NEWS.DATE_PUBLICATION_DEBUT.isNotNull())
                .and(Tables.NEWS.DATE_PUBLICATION_FIN.isNotNull())
                .and(Tables.NEWS.DATE_PUBLICATION_DEBUT.le(now))
                .and(Tables.NEWS.DATE_PUBLICATION_FIN.ge(now))
                .orderBy(Tables.NEWS.DATE_PUBLICATION_DEBUT.desc())
                .fetchInto(News.class);
        return allNews;
    }

    public void deleteById(UUID newsId) {
        // Delete pics associated to this news
        DSLContext context = newContext();
        context.deleteFrom(Tables.NEWS_PICTURE).where(Tables.NEWS_PICTURE.NEWS_ID.eq(newsId)).execute();
        context.deleteFrom(Tables.NEWS_LAKE).where(Tables.NEWS_LAKE.NEWS_ID.eq(newsId)).execute();
        // Delete news itself
        withDaoNoResult(NewsDao.class, dao -> dao.deleteById(newsId));
    }

    public void update(News news, Set<UUID> newsLakeIds) {
        withDaoNoResult(NewsLakeDao.class, dao -> {
            List<NewsLake> oldNewsLake = dao.fetchByNewsId(news.getId());
            Set<UUID> oldNewsLakeIds = oldNewsLake.stream()
                    .map(NewsLake::getLakeId)
                    .collect(Collectors.toSet());

            // Delete removed lakes
            oldNewsLake.stream()
                    .filter(fav -> !newsLakeIds.contains(fav.getLakeId()))
                    .forEach(dao::delete);

            // Add new lakes
            newsLakeIds.stream()
                    .filter(id -> !oldNewsLakeIds.contains(id))
                    .map(id -> new NewsLake(news.getId(), id))
                    .forEach(dao::insert);
        });
        withDaoNoResult(NewsDao.class, dao -> dao.update(news));
    }

    public News insert(News news, Set<UUID> newLakeIds) {
        DSLContext context = newContext();
        NewsRecord newRecord = context.newRecord(Tables.NEWS, news);
        News inserted = context.insertInto(Tables.NEWS)
                .set(newRecord)
                .returningResult(Tables.NEWS.ID)
                .fetchOne()
                .into(News.class);
        withDaoNoResult(NewsLakeDao.class, dao -> {
            newLakeIds.stream()
                    .map(id -> new NewsLake(inserted.getId(), id))
                    .forEach(dao::insert);
        });
        return inserted;
    }

    public NewsPicture insertNewsPicture(String newsId, String content, boolean isMiniature) {
        DSLContext dslContext = newContext();
        NewsPicture newsPicture = new NewsPicture();
        newsPicture.setIsMiniature(isMiniature);
        // Step 1: convert content to JPEG base 64 string
        String[] contentSplitted = content.split(",");
        String base64Image = contentSplitted[1].replace("\"", "");
        byte[] jpegBytes = ImageHelper.base64ImageToJpegBytes(base64Image, config.rawImageQuality());
        newsPicture.setContent(jpegBytes);

        // Step 2: assign temporary news id (for picture associated to not yet posted pic)
        if (newsId.equals("temp-id")) {
            newsPicture.setNewsId(TEMPORARY_NEWS_ID);
        } else {
            newsPicture.setNewsId(UUID.fromString(newsId));
        }

        // If miniature, delete previous one
        if (isMiniature) {
            dslContext.deleteFrom(Tables.NEWS_PICTURE).where(Tables.NEWS_PICTURE.NEWS_ID.eq(newsPicture.getNewsId()).and(Tables.NEWS_PICTURE.IS_MINIATURE.eq(true))).execute();
        }

        // Step 3: create pic and return id
        final NewsPicture inserted =  dslContext
                .insertInto(Tables.NEWS_PICTURE)
                .columns(Tables.NEWS_PICTURE.CONTENT, Tables.NEWS_PICTURE.NEWS_ID, Tables.NEWS_PICTURE.IS_MINIATURE)
                .values(newsPicture.getContent(), newsPicture.getNewsId(), isMiniature)
                .returningResult(Tables.NEWS_PICTURE.ID)
                .fetchOne()
                .into(NewsPicture.class);
        NewsPicture picture = withDao(NewsPictureDao.class, dao -> dao.findById(inserted.getId()));
        if (picture.getIsMiniature()) {
            // If miniature, update news to indicate url
            News news = findById(picture.getNewsId());
            if (news != null) {
                this.updateMiniatureId(news, picture.getId());
            }
        }

        return picture;

    }

    private void updateMiniatureId(News news, UUID miniatureId) {
        news.setMiniatureId(miniatureId);
        withDaoNoResult(NewsDao.class, dao -> dao.update(news));
    }

    public void updateTempNewsPictureIds(UUID newsId) {
        DSLContext dslContext = newContext();
        dslContext
            .update(Tables.NEWS_PICTURE)
            .set(Tables.NEWS_PICTURE.NEWS_ID, newsId)
            .where(Tables.NEWS_PICTURE.NEWS_ID.eq(TEMPORARY_NEWS_ID))
            .execute();

        List<Record1<UUID>> miniature = dslContext.select(Tables.NEWS_PICTURE.ID)
                .from(Tables.NEWS_PICTURE).where(Tables.NEWS_PICTURE.NEWS_ID.eq(newsId).and(Tables.NEWS_PICTURE.IS_MINIATURE
                .eq(true))).fetch().collect(Collectors.toList());
        if (!miniature.isEmpty()) {
            News news = findById(newsId);
            if (news != null) {
                this.updateMiniatureId(news, miniature.get(0).component1());
            }
        }
    }

    public News findById(UUID newsId) {
        return withDao(NewsDao.class, dao -> dao.findById(newsId));
    }

    public Optional<byte[]> getNewsPicture(UUID picId) {
        NewsPicture picture = withDao(NewsPictureDao.class, dao -> dao.findById(picId));
        Optional<byte[]> result = Optional.ofNullable(picture).map(NewsPicture::getContent);
        return result;
    }

    public NextScheduledCourrielNotificationCheck getNextScheduledNotificationCheck() {
        List<NextScheduledCourrielNotificationCheck> nextScheduledCourrielNotificationCheckDates = withDao(NextScheduledCourrielNotificationCheckDao.class, DAOImpl::findAll);
        if (nextScheduledCourrielNotificationCheckDates.isEmpty()) {
            // If no date defined, schedule for tomorrow at 7h30
            LocalDateTime nextSchedule = LocalDateTime.now();
            nextSchedule = nextSchedule.plusDays(1).withHour(7).withMinute(30).withSecond(0);
            NextScheduledCourrielNotificationCheck nextCheck = new NextScheduledCourrielNotificationCheck();
            nextCheck.setNextCheckDate(nextSchedule);
            withDaoNoResult(NextScheduledCourrielNotificationCheckDao.class, dao -> dao.insert(nextCheck));
            return nextCheck;
        } else {
            return nextScheduledCourrielNotificationCheckDates.iterator().next();
        }
    }

    public void scheduleNestNotificationCheck(int newsMailSendingDelayHours) {
        NextScheduledCourrielNotificationCheck nextScheduledNotificationCheck = getNextScheduledNotificationCheck();
        nextScheduledNotificationCheck.setNextCheckDate(nextScheduledNotificationCheck.getNextCheckDate().plusHours(newsMailSendingDelayHours));
        withDaoNoResult(NextScheduledCourrielNotificationCheckDao.class, dao -> dao.update(nextScheduledNotificationCheck));
    }

    public Set<UUID> getLakeIds(UUID newsId) {
        return withDao(NewsLakeDao.class, dao -> dao.fetchByNewsId(newsId).stream().map(NewsLake::getLakeId).collect(Collectors.toSet()));
    }

    public void notifySent(News news) {
        news.setDateNotificationSent(LocalDateTime.now());
        withDaoNoResult(NewsDao.class, dao -> dao.update(news));
    }
}
