package fr.inrae.fishola.database;

import fr.inrae.fishola.entities.tables.daos.NewsDao;
import fr.inrae.fishola.entities.tables.pojos.News;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Singleton;

@Singleton
public class NewsFisholaDao extends AbstractFisholaDao {

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
        withDaoNoResult(NewsDao.class, dao -> dao.deleteById(newsId));
    }

    public void update(News news) {
        withDaoNoResult(NewsDao.class, dao -> dao.update(news));
    }

    public void insert(News news) {
        withDaoNoResult(NewsDao.class, dao -> dao.insert(news));
    }

    public News findById(UUID newsId) {
        return withDao(NewsDao.class, dao -> dao.findById(newsId));
    }
}
