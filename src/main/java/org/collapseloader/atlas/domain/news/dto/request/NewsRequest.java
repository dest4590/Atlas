package org.collapseloader.atlas.domain.news.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsRequest {
    private String title;
    private String content;
    private String language;
}
