package com.homemakers.homemakers.model.event;

public class ComplaintEvent {

    private Long complaintId;
    private Long providerId;
    private Long bookingId;
    private ComplaintSeverity severity;
    private boolean validated;

    public ComplaintEvent(
            Long complaintId,
            Long providerId,
            Long bookingId,
            ComplaintSeverity severity,
            boolean validated
    ) {
        this.complaintId = complaintId;
        this.providerId = providerId;
        this.bookingId = bookingId;
        this.severity = severity;
        this.validated = validated;
    }

    public Long getComplaintId() {
        return complaintId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public ComplaintSeverity getSeverity() {
        return severity;
    }

    public boolean isValidated() {
        return validated;
    }
}
