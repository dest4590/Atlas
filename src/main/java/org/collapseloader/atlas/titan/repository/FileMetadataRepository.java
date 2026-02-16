package org.collapseloader.atlas.titan.repository;

import org.collapseloader.atlas.titan.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    Optional<FileMetadata> findByFilePath(String filePath);

    List<FileMetadata> findByDeletedTrue();

    List<FileMetadata> findByDeletedFalse();
}
