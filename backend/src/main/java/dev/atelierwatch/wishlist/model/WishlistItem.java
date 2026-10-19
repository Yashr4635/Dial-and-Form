package dev.atelierwatch.wishlist.model;

import dev.atelierwatch.auth.model.User;
import dev.atelierwatch.watches.model.Watch;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "wishlist_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "watch_id"})
})
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "watch_id", nullable = false)
    private Watch watch;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    public WishlistItem() {}

    public WishlistItem(User user, Watch watch) {
        this.user = user;
        this.watch = watch;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Watch getWatch() { return watch; }
    public void setWatch(Watch watch) { this.watch = watch; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
