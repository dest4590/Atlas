package org.collapseloader.atlas.domain.news;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<NewsDto>> getNews(@RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        String lang = parseLanguage(acceptLanguage);

        List<News> list = newsService.findByLanguage(lang);

        List<NewsDto> dtos = list.stream()
                .map(n -> new NewsDto(
                        n.getId(),
                        n.getTitle(),
                        n.getContent(),
                        n.getLanguage(),
                        n.getCreatedAt(),
                        n.getUpdatedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    private String parseLanguage(String header) {
        if (header == null || header.isBlank()) return "en";
        String[] parts = header.split(",");
        if (parts.length == 0) return "en";
        String token = parts[0].trim();
        try {
            return Locale.forLanguageTag(token).getLanguage();
        } catch (Exception e) {
            return token;
        }
    }
}
