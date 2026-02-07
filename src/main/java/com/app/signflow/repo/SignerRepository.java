package com.app.signflow.repo;

import com.app.signflow.model.entity.Signer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignerRepository extends JpaRepository<Signer, Long> {
    List<Signer> findByDocumentId(Long documentId);
}
