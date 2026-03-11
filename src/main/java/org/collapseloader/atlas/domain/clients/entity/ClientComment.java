package org.collapseloader.atlas.domain.clients.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.collapseloader.atlas.domain.users.entity.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Comment left by a user on a {@link Client}. Each entry stores the
 * text plus a reference to the author and the time it was created.
 */
@Entity
@Table(name = "client_comments")
@Data
public class ClientComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    private LocalDateTime createdAt;
}