package org.collapseloader.atlas.util;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;

public class CdnMetadataUtil {
    public record CdnMetadata(String md5, long sizeMb) {
    }

    public static CdnMetadata calculateMetadata(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                MessageDigest md = MessageDigest.getInstance("MD5");
                try (InputStream is = response.body()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytes = 0;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        md.update(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (byte b : md.digest()) {
                        sb.append(String.format("%02x", b));
                    }
                    return new CdnMetadata(sb.toString(), totalBytes / (1024 * 1024));
                }
            }
        } catch (Exception e) {
            System.err.println("Error calculating metadata for URL " + url + ": " + e.getMessage());
        }
        return null;
    }
}
