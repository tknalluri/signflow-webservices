package com.app.signflow.repo;

import com.app.signflow.model.entity.Document;
import com.app.signflow.model.entity.Document.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwnerId(Long ownerId);
    List<Document> findByOwnerIdAndStatus(Long ownerId, DocumentStatus status);
    
    @Query("SELECT d FROM Document d WHERE d.ownerId = :ownerId AND " +
           "(LOWER(d.fileName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Document> searchDocumentsByOwner(@Param("ownerId") Long ownerId, @Param("search") String search);
}
