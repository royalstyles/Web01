package com.jhpj.Web01.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "POST_LIKES",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_LIKES",
                columnNames = {"post_id", "user_id"}
        )
)
@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_likes_seq")
    @SequenceGenerator(name = "post_likes_seq", sequenceName = "POST_LIKES_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}