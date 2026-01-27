package org.collapseloader.atlas.domain.achievements.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "achievements")
@Getter
@Setter
@NoArgsConstructor
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String key;

    @Column(nullable = false)
    private String icon;

    @Column(nullable = false)
    private boolean hidden;

    public Achievement(String key, String icon, boolean hidden) {
        this.key = key;
        this.icon = icon;
        this.hidden = hidden;
    }
}
