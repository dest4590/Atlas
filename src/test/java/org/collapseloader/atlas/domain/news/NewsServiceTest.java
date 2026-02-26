package org.collapseloader.atlas.domain.news;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewsServiceTest {

    private NewsRepository newsRepository;
    private NewsService newsService;

    @BeforeEach
    void setUp() {
        newsRepository = mock(NewsRepository.class);
        newsService = new NewsService(newsRepository);
    }

    @Test
    void findAllReturnsRepositoryResult() {
        when(newsRepository.findAll()).thenReturn(List.of(new News()));

        var result = newsService.findAll();

        assertEquals(1, result.size());
        verify(newsRepository).findAll();
    }

    @Test
    void findByLanguageUsesFindAllWhenLanguageIsNull() {
        when(newsRepository.findAll()).thenReturn(List.of(new News()));

        var result = newsService.findByLanguage(null);

        assertEquals(1, result.size());
        verify(newsRepository).findAll();
        verify(newsRepository, never()).findByLanguage(any());
    }

    @Test
    void findByLanguageUsesFindAllWhenLanguageIsBlank() {
        when(newsRepository.findAll()).thenReturn(List.of(new News()));

        var result = newsService.findByLanguage("   ");

        assertEquals(1, result.size());
        verify(newsRepository).findAll();
        verify(newsRepository, never()).findByLanguage(any());
    }

    @Test
    void findByLanguageDelegatesToRepositoryForNonBlankLanguage() {
        when(newsRepository.findByLanguage("ru")).thenReturn(List.of(new News()));

        var result = newsService.findByLanguage("ru");

        assertEquals(1, result.size());
        verify(newsRepository).findByLanguage("ru");
    }

    @Test
    void createNewsDefaultsLanguageToEnWhenNull() {
        var request = new NewsRequest("Title", "Body", null);
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = newsService.createNews(request);

        assertEquals("Title", result.getTitle());
        assertEquals("Body", result.getContent());
        assertEquals("en", result.getLanguage());
        verify(newsRepository).save(any(News.class));
    }

    @Test
    void updateNewsUpdatesOnlyProvidedFields() throws Exception {
        var existing = new News();
        existing.setId(1L);
        existing.setTitle("Old");
        existing.setContent("OldContent");
        existing.setLanguage("en");

        var request = new NewsRequest("New", null, "ru");

        when(newsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(newsRepository.save(existing)).thenReturn(existing);

        var result = newsService.updateNews(1L, request);

        assertEquals("New", result.getTitle());
        assertEquals("OldContent", result.getContent());
        assertEquals("ru", result.getLanguage());
        verify(newsRepository).save(existing);
    }

    @Test
    void updateNewsThrowsWhenEntityMissing() {
        when(newsRepository.findById(55L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> newsService.updateNews(55L, new NewsRequest()));
        verify(newsRepository, never()).save(any());
    }

    @Test
    void deleteNewsDeletesWhenExists() throws Exception {
        when(newsRepository.existsById(10L)).thenReturn(true);

        newsService.deleteNews(10L);

        verify(newsRepository).deleteById(10L);
    }

    @Test
    void deleteNewsThrowsWhenMissing() {
        when(newsRepository.existsById(11L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> newsService.deleteNews(11L));
        verify(newsRepository, never()).deleteById(any());
    }
}
