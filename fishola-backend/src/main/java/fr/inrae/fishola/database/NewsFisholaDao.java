package fr.inrae.fishola.database;

import fr.inrae.fishola.entities.Tables;
import fr.inrae.fishola.entities.tables.daos.NewsDao;
import fr.inrae.fishola.entities.tables.daos.NewsPictureDao;
import fr.inrae.fishola.entities.tables.daos.NextScheduledCourrielNotificationCheckDao;
import fr.inrae.fishola.entities.tables.pojos.News;
import fr.inrae.fishola.entities.tables.pojos.NewsPicture;
import fr.inrae.fishola.entities.tables.pojos.NextScheduledCourrielNotificationCheck;
import fr.inrae.fishola.entities.tables.records.NewsRecord;
import fr.inrae.fishola.rest.ImageHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import org.jooq.DSLContext;

@Singleton
public class NewsFisholaDao extends AbstractFisholaDao {

    public static final UUID TEMPORARY_NEWS_ID = UUID.randomUUID();

    public List<News> getNews(boolean onlyListPublishedNews) {
        List<News> allNews = withDao(NewsDao.class, dao -> dao.findAll());
        if (onlyListPublishedNews) {
            LocalDateTime now = LocalDateTime.now();
            return allNews.stream().filter(
                    news ->
                            news.getDatePublicationDebut() != null && now.isAfter(news.getDatePublicationDebut()) &&
                            news.getDatePublicationFin() != null && now.isBefore(news.getDatePublicationFin())
            ).collect(Collectors.toList());
        }
        return allNews;
    }

    public void deleteById(UUID newsId) {
        // Delete pics associated to this news
        DSLContext context = newContext();
        context.deleteFrom(Tables.NEWS_PICTURE).where(Tables.NEWS_PICTURE.NEWS_ID.eq(newsId)).execute();
        // Delete news itself
        withDaoNoResult(NewsDao.class, dao -> dao.deleteById(newsId));
    }

    public void update(News news) {
        withDaoNoResult(NewsDao.class, dao -> dao.update(news));
    }

    public News insert(News news) {
        DSLContext context = newContext();
        NewsRecord record = context.newRecord(Tables.NEWS, news);
        News inserted = context.insertInto(Tables.NEWS)
                .set(record)
                .returningResult(Tables.NEWS.ID)
                .fetchOne()
                .into(News.class);
        return inserted;
    }

    public NewsPicture insertNewsPicture(String newsId, String content) {
        DSLContext dslContext = newContext();
        NewsPicture newsPicture = new NewsPicture();
        // Step 1: convert content to JPEG base 64 string
        String[] contentSplitted = content.split(",");
        String base64Image = contentSplitted[1].replaceAll("\"", "");
        byte[] jpegBytes = ImageHelper.base64ImageToJpegBytes(base64Image, config.rawImageQuality());
        newsPicture.setContent(jpegBytes);

        // Step 2: assign temporary news id (for picture associated to not yet posted pic)
        if (newsId.equals("temp-id")) {
            newsPicture.setNewsId(TEMPORARY_NEWS_ID);
        } else {
            newsPicture.setNewsId(UUID.fromString(newsId));
        }

        // Step 3: create pic and return id
        return dslContext
                .insertInto(Tables.NEWS_PICTURE)
                .columns(Tables.NEWS_PICTURE.CONTENT, Tables.NEWS_PICTURE.NEWS_ID)
                .values(newsPicture.getContent(), newsPicture.getNewsId())
                .returningResult(Tables.NEWS_PICTURE.ID)
                .fetchOne()
                .into(NewsPicture.class);

    }

    public void updateTempNewsPictureIds(UUID newsPictureId) {
        DSLContext dslContext = newContext();
        dslContext
            .update(Tables.NEWS_PICTURE)
            .set(Tables.NEWS_PICTURE.NEWS_ID, newsPictureId)
            .where(Tables.NEWS_PICTURE.NEWS_ID.eq(TEMPORARY_NEWS_ID))
            .execute();
    }

    public News findById(UUID newsId) {
        return withDao(NewsDao.class, dao -> dao.findById(newsId));
    }

    public Optional<byte[]> getNewsPicture(UUID picId) {
        NewsPicture picture = withDao(NewsPictureDao.class, dao -> dao.findById(picId));
        Optional<byte[]> result = Optional.ofNullable(picture).map(NewsPicture::getContent);
        return result;
    }

    public LocalDateTime getNextScheduledNotificationCheckDate() {
        List<NextScheduledCourrielNotificationCheck> nextScheduledCourrielNotificationCheckDates = withDao(NextScheduledCourrielNotificationCheckDao.class, dao -> dao.findAll());
        if (nextScheduledCourrielNotificationCheckDates.isEmpty()) {
            // If no date defined, schedule for tomorrow at 7h30
            LocalDateTime nextSchedule = LocalDateTime.now();
            nextSchedule = nextSchedule.plusDays(1).withHour(7).withMinute(30).withSecond(0);
            NextScheduledCourrielNotificationCheck nextCheck = new NextScheduledCourrielNotificationCheck();
            nextCheck.setNextCheckDate(nextSchedule);
            withDaoNoResult(NextScheduledCourrielNotificationCheckDao.class, dao -> dao.insert(nextCheck));
            return nextSchedule;
        } else {
            return nextScheduledCourrielNotificationCheckDates.iterator().next().getNextCheckDate();
        }
    }
}
