package com.homemakers.homemakers.dto;

import com.homemakers.homemakers.model.BookingStatus;
import com.homemakers.homemakers.model.ServiceType;

import java.util.List;

/**
 * DTO returned by the provider bookings endpoint and the single-booking endpoint.
 *
 * Uses a private all-args constructor + public static {@link Builder} so callers
 * never have to count positional arguments — adding a new field is a one-liner
 * and never breaks existing call sites.
 */
public class BookingResponse {

    private final Long              bookingId;
    private final BookingStatus     status;

    private final String            customerName;
    private final String            customerPhone;      // null until CONFIRMED or beyond
    private final String            serviceAddress;
    private final String            customerNote;
    private final String            terminationReason;  // ✅ populated only when TERMINATED

    private final String            serviceDate;
    private final String            startTime;
    private final String            endTime;

    private final String            paymentStatus;
    private final Double            totalAmount;

    private final int               totalDays;
    private final int               chargeableDays;
    private final int               holidays;

    private final List<ServiceType> services;

    // ── Private constructor (use Builder) ─────────────────────────────────────

    private BookingResponse(Builder b) {
        this.bookingId         = b.bookingId;
        this.status            = b.status;
        this.customerName      = b.customerName;
        this.customerPhone     = b.customerPhone;
        this.serviceAddress    = b.serviceAddress;
        this.customerNote      = b.customerNote;
        this.terminationReason = b.terminationReason;
        this.serviceDate       = b.serviceDate;
        this.startTime         = b.startTime;
        this.endTime           = b.endTime;
        this.paymentStatus     = b.paymentStatus;
        this.totalAmount       = b.totalAmount;
        this.totalDays         = b.totalDays;
        this.chargeableDays    = b.chargeableDays;
        this.holidays          = b.holidays;
        this.services          = b.services;
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long              bookingId;
        private BookingStatus     status;
        private String            customerName;
        private String            customerPhone;
        private String            serviceAddress;
        private String            customerNote;
        private String            terminationReason;
        private String            serviceDate;
        private String            startTime;
        private String            endTime;
        private String            paymentStatus;
        private Double            totalAmount;
        private int               totalDays;
        private int               chargeableDays;
        private int               holidays;
        private List<ServiceType> services;

        private Builder() {}

        public Builder bookingId(Long val)              { bookingId         = val; return this; }
        public Builder status(BookingStatus val)        { status            = val; return this; }
        public Builder customerName(String val)         { customerName      = val; return this; }
        public Builder customerPhone(String val)        { customerPhone     = val; return this; }
        public Builder serviceAddress(String val)       { serviceAddress    = val; return this; }
        public Builder customerNote(String val)         { customerNote      = val; return this; }
        public Builder terminationReason(String val)    { terminationReason = val; return this; }
        public Builder serviceDate(String val)          { serviceDate       = val; return this; }
        public Builder startTime(String val)            { startTime         = val; return this; }
        public Builder endTime(String val)              { endTime           = val; return this; }
        public Builder paymentStatus(String val)        { paymentStatus     = val; return this; }
        public Builder totalAmount(Double val)          { totalAmount       = val; return this; }
        public Builder totalDays(int val)               { totalDays         = val; return this; }
        public Builder chargeableDays(int val)          { chargeableDays    = val; return this; }
        public Builder holidays(int val)                { holidays          = val; return this; }
        public Builder services(List<ServiceType> val)  { services          = val; return this; }

        public BookingResponse build()                  { return new BookingResponse(this); }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long              getBookingId()         { return bookingId;         }
    public BookingStatus     getStatus()            { return status;            }
    public String            getCustomerName()      { return customerName;      }
    public String            getCustomerPhone()     { return customerPhone;     }
    public String            getServiceAddress()    { return serviceAddress;    }
    public String            getCustomerNote()      { return customerNote;      }
    public String            getTerminationReason() { return terminationReason; }
    public String            getServiceDate()       { return serviceDate;       }
    public String            getStartTime()         { return startTime;         }
    public String            getEndTime()           { return endTime;           }
    public String            getPaymentStatus()     { return paymentStatus;     }
    public Double            getTotalAmount()       { return totalAmount;       }
    public int               getTotalDays()         { return totalDays;         }
    public int               getChargeableDays()    { return chargeableDays;    }
    public int               getHolidays()          { return holidays;          }
    public List<ServiceType> getServices()          { return services;          }
}