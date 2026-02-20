package org.collapseloader.atlas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@ConditionalOnProperty(name = "atlas.backup.s3.enabled", havingValue = "true")
public class BackupService {

    private final String bucketName;
    private final String dbUser;
    private final String dbPassword;
    private final String datasourceUrl;
    private final String pgDumpPath;
    private final boolean useDumpall;
    private final int retentionDays;
    private final S3Client s3Client;

    public BackupService(
            @Value("${atlas.backup.s3.bucket}") String bucketName,
            @Value("${atlas.backup.s3.region:us-east-1}") String region,
            @Value("${atlas.backup.s3.endpoint:}") String endpoint,
            @Value("${atlas.backup.s3.access-key}") String accessKey,
            @Value("${atlas.backup.s3.secret-key}") String secretKey,
            @Value("${atlas.backup.s3.retention-days:7}") int retentionDays,
            @Value("${atlas.backup.s3.pg-dump-path:pg_dump}") String pgDumpPath,
            @Value("${atlas.backup.s3.use-dumpall:false}") boolean useDumpall,
            @Value("${spring.datasource.username}") String dbUser,
            @Value("${spring.datasource.password}") String dbPassword,
            @Value("${spring.datasource.url}") String datasourceUrl) {

        this.bucketName = bucketName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.datasourceUrl = datasourceUrl;
        this.retentionDays = retentionDays;
        this.pgDumpPath = pgDumpPath;
        this.useDumpall = useDumpall;

        var credentials = AwsBasicCredentials.create(accessKey, secretKey);
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());

        if (endpoint != null && !endpoint.isEmpty()) {
            log.info("Using S3 endpoint override: {}", endpoint);
            builder.endpointOverride(URI.create(endpoint));
        }

        this.s3Client = builder.build();
    }

    @Scheduled(cron = "${atlas.backup.s3.cron:0 0 3 * * *}")
    public void scheduleBackup() {
        log.info("Starting scheduled database backup to S3 bucket: {}", bucketName);
        try {
            performBackup();
            performCleanup();
        } catch (Exception e) {
            log.error("Failed to perform scheduled backup to S3: {}", e.getMessage(), e);
        }
    }

    public void performCleanup() {
        log.info("Cleaning up backups older than {} days from S3 bucket: {}", retentionDays, bucketName);
        try {
            Instant threshold = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            for (S3Object s3Object : listResponse.contents()) {
                if (s3Object.lastModified().isBefore(threshold)) {
                    log.info("Deleting old backup from S3: {}", s3Object.key());
                    DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Object.key())
                            .build();
                    s3Client.deleteObject(deleteRequest);
                }
            }
        } catch (Exception e) {
            log.error("Error during S3 cleanup: {}", e.getMessage(), e);
        }
    }

    public void performBackup() throws IOException, InterruptedException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String fileName = "atlas_backup_" + timestamp + ".sql";
        File tempFile = File.createTempFile("atlas_backup_", ".sql");

        try {
            log.info("Creating database dump to {}", tempFile.getAbsolutePath());

            List<String> command = new ArrayList<>();

            command.add(pgDumpPath);
            command.add("-h");
            command.add(getDbHost());
            command.add("-p");
            command.add(getDbPort());
            command.add("-U");
            command.add(dbUser);
            command.add("-f");
            command.add(tempFile.getAbsolutePath());

            command.add("--clean");
            command.add("--if-exists");
            command.add("--inserts");
            command.add(getDbName());

            ProcessBuilder processBuilder = new ProcessBuilder(command);

            processBuilder.environment().put("PGPASSWORD", dbPassword);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                throw new IOException("pg_dump process timed out");
            }

            if (process.exitValue() != 0) {
                String errorOutput = new String(process.getInputStream().readAllBytes());
                throw new IOException("pg_dump failed with code " + process.exitValue() + ": " + errorOutput);
            }

            log.info("Backup dump successful, file size: {} bytes. Uploading to S3...", tempFile.length());

            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucketName).key(fileName).build(),
                    RequestBody.fromFile(tempFile));

            log.info("Successfully uploaded backup {} to S3", fileName);

        } finally {
            if (tempFile.exists() && !tempFile.delete()) {
                log.warn("Failed to delete temporary backup file: {}", tempFile.getAbsolutePath());
            }
        }
    }

    private String getDbHost() {
        try {
            String part = datasourceUrl.substring(datasourceUrl.indexOf("//") + 2);
            String hostPortPart = part.substring(0, part.indexOf("/"));
            if (hostPortPart.contains(":")) {
                return hostPortPart.split(":")[0];
            }
            return hostPortPart;
        } catch (Exception e) {
            log.error("Failed to parse DB host from URL: {}", datasourceUrl);
            return "localhost";
        }
    }

    private String getDbPort() {
        try {
            String part = datasourceUrl.substring(datasourceUrl.indexOf("//") + 2);
            String hostPortPart = part.substring(0, part.indexOf("/"));
            if (hostPortPart.contains(":")) {
                return hostPortPart.split(":")[1];
            }
            return "5432";
        } catch (Exception e) {
            log.warn("Failed to parse DB port from URL, defaulting to 5432. URL: {}", datasourceUrl);
            return "5432";
        }
    }

    private String getDbName() {
        try {
            String part = datasourceUrl.substring(datasourceUrl.indexOf("//") + 2);
            String dbPath = part.substring(part.indexOf("/") + 1);
            if (dbPath.contains("?")) {
                return dbPath.substring(0, dbPath.indexOf("?"));
            }
            return dbPath;
        } catch (Exception e) {
            log.error("Failed to parse DB name from URL: {}", datasourceUrl);
            return "atlas";
        }
    }
}