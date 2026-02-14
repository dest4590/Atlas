package org.collapseloader.atlas.domain.news;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsService {

    private final NewsRepository newsRepository;

    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @Cacheable("news_list")
    public List<News> findAll() {
        return newsRepository.findAll();
    }

    @Cacheable("news_list")
    public List<News> findByLanguage(String language) {
        if (language == null || language.isBlank()) {
            return findAll();
        }
        return newsRepository.findByLanguage(language);
    }

    @CacheEvict(value = "news_list", allEntries = true)
    public void save(News news) {
        newsRepository.save(news);
    }

    @CacheEvict(value = "news_list", allEntries = true)
    public News createNews(NewsRequest request) {
        News news = new News();
        news.setTitle(request.getTitle());
        news.setContent(request.getContent());
        news.setLanguage(request.getLanguage() != null ? request.getLanguage() : "en");
        return newsRepository.save(news);
    }

    @CacheEvict(value = "news_list", allEntries = true)
    public News updateNews(Long id, NewsRequest request) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));
        if (request.getTitle() != null)
            news.setTitle(request.getTitle());
        if (request.getContent() != null)
            news.setContent(request.getContent());
        if (request.getLanguage() != null)
            news.setLanguage(request.getLanguage());
        return newsRepository.save(news);
    }

    @CacheEvict(value = "news_list", allEntries = true)
    public void deleteNews(Long id) {
        if (!newsRepository.existsById(id)) {
            throw new RuntimeException("News not found");
        }
        newsRepository.deleteById(id);
    }
}
