package org.collapseloader.atlas.domain.storage.repository;

import org.collapseloader.atlas.domain.storage.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    Optional<FileMetadata> findByFilePath(String filePath);

    void deleteByFilePath(String filePath);
}
