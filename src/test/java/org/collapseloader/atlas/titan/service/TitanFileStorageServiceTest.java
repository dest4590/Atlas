package org.collapseloader.atlas.titan.service;

import org.collapseloader.atlas.config.StorageProperties;
import org.collapseloader.atlas.exception.TitanException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TitanFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private TitanFileStorageService storageService;
    private FileMetadataService metadataService;

    private static String md5(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(payload));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] concat(byte[] first, byte[] second) throws IOException {
        byte[] merged = new byte[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setUploadDir(tempDir.toString());
        properties.setTempDir("temp");
        properties.setTrashDir(".trash");

        metadataService = mock(FileMetadataService.class);
        when(metadataService.findAll()).thenReturn(List.of());
        when(metadataService.findTrash()).thenReturn(List.of());

        storageService = new TitanFileStorageService(properties, metadataService);
        storageService.init();
    }

    @Test
    void storeWritesFileAndSavesMd5InSinglePass() throws Exception {
        byte[] payload = "hello titan storage".getBytes(StandardCharsets.UTF_8);
        MultipartFile file = new MockMultipartFile("file", "upload.bin", "application/octet-stream", payload);

        TitanFileStorageService.StoredFile stored = storageService.store(file, "clients", "saved.bin");

        Path storedPath = storageService.getRootLocation().resolve("clients/saved.bin");
        assertTrue(Files.exists(storedPath));
        assertArrayEquals(payload, Files.readAllBytes(storedPath));
        assertEquals("clients/saved.bin", stored.storedPath());

        String expectedMd5 = md5(payload);
        assertEquals(expectedMd5, stored.md5());

        verify(metadataService).saveCalculatedMd5(
                eq(storedPath.toAbsolutePath().normalize()),
                eq(storageService.getRootLocation()),
                eq(expectedMd5),
                eq((long) payload.length),
                anyLong());
    }

    @Test
    void mergeChunksMergesInOrderAndStoresMd5() throws Exception {
        String uploadId = "upload-ok";
        byte[] chunk0 = "part-0-".getBytes(StandardCharsets.UTF_8);
        byte[] chunk1 = "part-1".getBytes(StandardCharsets.UTF_8);

        storageService.storeChunk(uploadId, 0,
                new MockMultipartFile("file", "0", "application/octet-stream", chunk0));
        storageService.storeChunk(uploadId, 1,
                new MockMultipartFile("file", "1", "application/octet-stream", chunk1));

        TitanFileStorageService.StoredFile stored = storageService.mergeChunks(uploadId, "merged.bin", "", 2);

        byte[] merged = concat(chunk0, chunk1);
        Path destination = storageService.getRootLocation().resolve("merged.bin");
        assertTrue(Files.exists(destination));
        assertArrayEquals(merged, Files.readAllBytes(destination));
        assertEquals(md5(merged), stored.md5());

        Path uploadTempDir = storageService.getRootLocation().resolve("temp").resolve(uploadId);
        assertFalse(Files.exists(uploadTempDir));

        verify(metadataService).saveCalculatedMd5(
                eq(destination.toAbsolutePath().normalize()),
                eq(storageService.getRootLocation()),
                eq(md5(merged)),
                eq((long) merged.length),
                anyLong());
    }

    @Test
    void mergeChunksFailsFastWhenChunkIsMissing() {
        String uploadId = "upload-missing";
        storageService.storeChunk(uploadId, 0,
                new MockMultipartFile("file", "0", "application/octet-stream",
                        "chunk-0".getBytes(StandardCharsets.UTF_8)));

        TitanException exception = assertThrows(TitanException.class,
                () -> storageService.mergeChunks(uploadId, "broken.bin", "", 2));

        assertTrue(exception.getMessage().contains("Missing chunks"));
        assertTrue(exception.getMessage().contains("1"));
        assertFalse(Files.exists(storageService.getRootLocation().resolve("broken.bin")));
    }

    @Test
    void storeRejectsPathTraversalSubdir() {
        MultipartFile file = new MockMultipartFile("file", "upload.bin", "application/octet-stream",
                "x".getBytes(StandardCharsets.UTF_8));

        TitanException exception = assertThrows(TitanException.class,
                () -> storageService.store(file, "../outside", "a.bin"));

        assertTrue(exception.getMessage().contains("outside current directory"));
    }
}
