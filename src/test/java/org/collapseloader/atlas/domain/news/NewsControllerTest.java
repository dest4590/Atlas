package org.collapseloader.atlas.domain.news;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewsControllerTest {

    private NewsService newsService;
    private NewsController newsController;

    @BeforeEach
    void setUp() {
        newsService = mock(NewsService.class);
        newsController = new NewsController(newsService);
    }

    @Test
    void getNewsWithoutLanguageHeaderDefaultsToEnglish() {
        var news = newsItem(1L, "Title", "Content", "en");
        when(newsService.findByLanguage("en")).thenReturn(List.of(news));

        ResponseEntity<List<NewsDto>> response = newsController.getNews(null);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Title", response.getBody().getFirst().getTitle());
        verify(newsService).findByLanguage("en");
    }

    @Test
    void getNewsParsesLanguageFromAcceptLanguageHeader() {
        when(newsService.findByLanguage("ru")).thenReturn(List.of());

        ResponseEntity<List<NewsDto>> response = newsController.getNews("ru-RU,ru;q=0.9,en;q=0.8");

        assertEquals(200, response.getStatusCode().value());
        verify(newsService).findByLanguage("ru");
    }

    private News newsItem(Long id, String title, String content, String language) {
        var news = new News();
        news.setId(id);
        news.setTitle(title);
        news.setContent(content);
        news.setLanguage(language);
        news.setCreatedAt(Instant.now());
        news.setUpdatedAt(Instant.now());
        return news;
    }
}
