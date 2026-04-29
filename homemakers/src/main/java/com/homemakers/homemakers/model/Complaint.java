package com.homemakers.homemakers.model;

import com.homemakers.homemakers.model.event.ComplaintSeverity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @ManyToOne(optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    private ComplaintSeverity severity;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status; // PENDING, VALIDATED, REJECTED

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    @Column(length = 1000)
    private String adminNote;

    // Getters & Setters
    public Long getId()                        { return id; }
    public User getUser()                      { return user; }
    public void setUser(User user)             { this.user = user; }
    public Provider getProvider()              { return provider; }
    public void setProvider(Provider p)        { this.provider = p; }
    public Booking getBooking()                { return booking; }
    public void setBooking(Booking b)          { this.booking = b; }
    public ComplaintSeverity getSeverity()     { return severity; }
    public void setSeverity(ComplaintSeverity s) { this.severity = s; }
    public String getDescription()             { return description; }
    public void setDescription(String d)       { this.description = d; }
    public ComplaintStatus getStatus()         { return status; }
    public void setStatus(ComplaintStatus s)   { this.status = s; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime t)  { this.createdAt = t; }
    public LocalDateTime getResolvedAt()       { return resolvedAt; }
    public void setResolvedAt(LocalDateTime t) { this.resolvedAt = t; }
    public String getAdminNote()               { return adminNote; }
    public void setAdminNote(String n)         { this.adminNote = n; }
}