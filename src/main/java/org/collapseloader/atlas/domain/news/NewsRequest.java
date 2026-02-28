package org.collapseloader.atlas.domain.news;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsRequest {
    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;
    @Size(max = 10000, message = "content must be at most 10000 characters")
    private String content;
    @Pattern(regexp = "^[a-zA-Z]{2,8}([-_][a-zA-Z]{2,8})?$", message = "language must be a valid language tag")
    private String language;
}
