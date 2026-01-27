package org.collapseloader.atlas.domain.clients.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.collapseloader.atlas.domain.users.entity.User;

@Entity
@Table(name = "client_ratings", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "client_id", "user_id" })
})
@Data
public class ClientRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private double rating;
}
