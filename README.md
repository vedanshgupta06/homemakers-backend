# TheHomemakers — Backend

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.8-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Render](https://img.shields.io/badge/Deployed_on-Render-46E3B7?style=for-the-badge&logo=render&logoColor=white)

**REST API backend for TheHomemakers — a full-stack home services booking platform.**

[Live API](https://homemakers-backend.onrender.com) · [Frontend Repo](https://github.com/vedanshgupta06/homemakers-frontend) · [Live Demo](https://homemakers-frontend.vercel.app)

</div>

---

## About

TheHomemakers is a production-grade home services marketplace where users can book trusted professionals for cleaning, cooking, babysitting, laundry, and more. The backend handles the full booking lifecycle, dual payment processing, wallet management, provider earnings, and automated scheduling.

---

## Features

**Authentication & Security**
- JWT-based authentication with access + refresh tokens
- Role-based access control — USER, PROVIDER, ADMIN
- Spring Security with stateless session management

**Booking System**
- Full booking lifecycle: PENDING → CONFIRMED → COMPLETED → CANCELLED
- Provider availability slot management with overlap detection
- Auto-cancellation of expired bookings via scheduled jobs
- 30-day slot locking on booking confirmation

**Payment Processing**
- Stripe integration for card payments
- Razorpay integration for UPI/Netbanking
- Wallet system with balance, reservations, and refunds
- Automatic refund on cancellation (Stripe → Wallet)

**Provider Management**
- Provider onboarding with document verification
- Earnings tracking and payout requests
- Penalty system for no-shows (₹200 deduction)
- Weekly settlement reports

**Scheduled Jobs**
- Auto-cancel PENDING bookings older than 2 days
- Auto-cancel CONFIRMED bookings whose scheduled time has passed
- 2-hour and 15-minute reminders before service start
- Safety net for missed refunds on cancelled bookings

**Admin Panel**
- Platform analytics (revenue, bookings, providers)
- Monthly revenue charts and service distribution
- Provider management and complaint handling
- Payout approval workflow

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.5.8 |
| Language | Java 17 |
| Database | MySQL 8.0 |
| ORM | Hibernate / Spring Data JPA |
| Security | Spring Security + JWT (JJWT 0.11.5) |
| Payments | Stripe Java SDK + Razorpay Java SDK |
| Containerization | Docker (multi-stage build) |
| Deployment | Render (Free tier) |
| Database Host | Railway MySQL |

---

## Architecture

```
homemakers-backend/
├── controller/          # REST endpoints (Auth, Booking, Payment, Admin...)
├── service/             # Business logic
│   ├── BookingService.java
│   ├── BookingExpiryService.java   # Scheduled jobs
│   ├── UserWalletService.java
│   ├── ProviderEarningService.java
│   └── ...
├── model/               # JPA entities
├── repository/          # Spring Data JPA repositories
├── config/              # Security, CORS, JWT config
├── dto/                 # Data Transfer Objects
└── mapper/              # Safe entity → DTO mapping (fixes lazy loading)
```

---

## Key Design Decisions

**BookingMapper** — All booking responses go through a dedicated mapper to safely convert JPA entities to DTOs, preventing Hibernate lazy loading exceptions and ByteBuddy proxy serialization issues.

**@Transactional on all booking endpoints** — Ensures atomicity across wallet deductions, payment recording, slot locking, and booking status updates.

**Staggered scheduler startup** — Both scheduled jobs use `initialDelay` to avoid DB connection race conditions on cold starts.

**Wallet reservation pattern** — On booking creation, wallet funds are reserved (not deducted). On cancellation, they are released. On confirmation + payment, they are consumed. This prevents double-spending.

---

## API Endpoints

| Module | Base Path |
|---|---|
| Auth | `/api/auth` |
| User | `/api/user` |
| Booking | `/api/bookings` |
| Provider | `/api/provider` |
| Stripe | `/api/stripe` |
| Razorpay | `/api/razorpay` |
| Admin | `/api/admin` |
| Wallet | `/api/wallet` |

---

## Local Setup

**Prerequisites:** Java 17, Maven, MySQL 8.0

```bash
# Clone the repo
git clone https://github.com/vedanshgupta06/homemakers-backend.git
cd homemakers-backend/homemakers

# Create local database
mysql -u root -p
CREATE DATABASE homemakers_db;

# Run the application
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.

Default credentials are read from `application.properties` fallback values for local development.

---

## Environment Variables

| Variable | Description |
|---|---|
| `DB_URL` | JDBC MySQL connection URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | HS384 signing secret (64+ chars) |
| `STRIPE_SECRET_KEY` | Stripe secret key |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret |
| `RAZORPAY_KEY_ID` | Razorpay key ID |
| `RAZORPAY_KEY_SECRET` | Razorpay key secret |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins |
| `APP_BASE_URL` | Public URL of this service |

---

## Deployment

The backend is containerized with a multi-stage Dockerfile and deployed on Render.

```dockerfile
# Stage 1 — Build
FROM eclipse-temurin:17-jdk-alpine AS build
# Maven build with dependency caching

# Stage 2 — Run  
FROM eclipse-temurin:17-jre-alpine
# Minimal JRE image with the built JAR
```

**Database** is hosted on Railway MySQL with the production data migrated from local.

---

## Related

- [Frontend Repository](https://github.com/vedanshgupta06/homemakers-frontend) — React + Vite + Tailwind
- [Live Demo](https://homemakers-frontend.vercel.app)

---

<div align="center">
Made by <a href="https://github.com/vedanshgupta06">Vedansh Gupta</a>
</div>
