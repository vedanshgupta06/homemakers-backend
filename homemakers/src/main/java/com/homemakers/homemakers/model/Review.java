package com.homemakers.homemakers.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"booking_id"})
        }
)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // USER who wrote the review
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // PROVIDER who received the review
    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    // Booking being reviewed (ONE review per booking)
    @OneToOne(optional = false)
    @JoinColumn(name = "booking_id", unique = true)
    private Booking booking;

    // Rating 1–5
    private int rating;

    @Column(length = 1000)
    private String comment;

    // ===== GETTERS & SETTERS =====

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
