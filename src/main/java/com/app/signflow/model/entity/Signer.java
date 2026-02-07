package com.app.signflow.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "signers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Signer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignerStatus status;

    @Column
    private LocalDateTime signedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = SignerStatus.PENDING;
        }
    }

    public enum SignerStatus {
        PENDING, SIGNED
    }
}
