package org.collapseloader.atlas.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "atlas.storage")
public class StorageProperties {
    private String uploadDir = "uploads";
    private String tempDir = "temp";
    private String trashDir = ".trash";
    private int trashRetentionDays = 30;
}
