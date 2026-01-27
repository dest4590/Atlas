package org.collapseloader.atlas.domain.news;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class NewsDataLoader implements CommandLineRunner {

    private final NewsService newsService;

    public NewsDataLoader(NewsService newsService) {
        this.newsService = newsService;
    }

    @Override
    public void run(String @NonNull ... args) {
        if (newsService.findAll().isEmpty()) {
            News n1 = new News();
            n1.setTitle("Welcome to Atlas");
            n1.setContent("Thanks for trying Atlas — this is a sample news article.");
            n1.setLanguage("en");
            newsService.save(n1);

            News n2 = new News();
            n2.setTitle("Добро пожаловать в Atlas");
            n2.setContent("Спасибо за использование Atlas — это демонстрационная новость.");
            n2.setLanguage("ru");
            newsService.save(n2);
        }
    }
}
