# StockSense - Enterprise Inventory Management System

StockSense is a robust, full-stack Enterprise Inventory Management System designed to provide real-time inventory tracking, multi-warehouse operational management, double-entry stock ledger auditing, and automated replenishment workflows.

Built with Java 21, Spring Boot 3.3.4, PostgreSQL, and React with TypeScript, StockSense provides enterprise-grade scalability, immutable inventory event tracking, and intuitive user workflows for warehouse managers and operational staff.

---

## Table of Contents

- [System Architecture](#system-architecture)
- [Use Case & Role Modeling](#use-case--role-modeling)
- [Core Features](#core-features)
- [Technology Stack](#technology-stack)
- [Project Directory Structure](#project-directory-structure)
- [Data Integrity & Stock Ledger Model](#data-integrity--stock-ledger-model)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Database Setup with Docker](#database-setup-with-docker)
  - [Backend Setup](#backend-setup)
  - [Frontend Setup](#frontend-setup)
- [REST API Reference](#rest-api-reference)
- [Security & Authentication](#security--authentication)

---

## System Architecture

StockSense follows a multi-tier decoupled architecture with a React Single-Page Application (SPA) frontend, a Spring Boot RESTful API backend layer, and a PostgreSQL relational database tier with dedicated immutability constraints for transaction ledgers.

```mermaid
graph TD
    subgraph Client Tier
        UI[React Single Page Application]
        State[React Hooks & State Management]
        AxiosClient[Axios HTTP Client Service]
    end

    subgraph Security Layer
        AuthFilter[Auth Interceptor & Token Verifier]
        UserCtx[Thread-Local User Context]
    end

    subgraph Backend Application Tier (Spring Boot 3.3.4 / Java 21)
        REST[REST Controllers]
        
        subgraph Domain Business Services
            AuthSvc[Auth & Email Notification Service]
            ProductSvc[Product & Inventory Service]
            WarehouseSvc[Warehouse & Location Service]
            ReceiptSvc[Receipt Order Service]
            DeliverySvc[Delivery Order Service]
            TransferSvc[Internal Transfer Service]
            OperationSvc[Stock Operation & Adjustment Service]
            LedgerSvc[Immutable Stock Ledger Service]
            DashboardSvc[Analytics & KPI Dashboard Service]
        end

        JPA[Spring Data JPA Repositories]
    end

    subgraph Persistence Tier
        DB[(PostgreSQL 16 Database)]
        LedgerTable[Immutable Stock Ledger Audit Log]
    end

    UI --> State
    State --> AxiosClient
    AxiosClient -->|HTTPS REST API / JSON| REST
    REST --> AuthFilter
    AuthFilter --> UserCtx
    UserCtx --> Domain Business Services
    Domain Business Services --> JPA
    JPA --> DB
    LedgerSvc --> LedgerTable
```

---

## Use Case & Role Modeling

The system establishes strict Role-Based Access Control (RBAC) separating administrative warehouse management functions from day-to-day warehouse operations.

```mermaid
graph LR
    subgraph System Roles
        Manager[Warehouse Manager]
        Worker[Warehouse Staff / Worker]
    end

    subgraph Core System Use Cases
        UC1[Account Registration & Authentication]
        UC2[Dashboard Analytics & Real-Time KPIs]
        UC3[Manage Product Catalog & Thresholds]
        UC4[Manage Warehouses & Storage Locations]
        UC5[Process Supplier Receipts & Inbound Logistics]
        UC6[Process Customer Deliveries & Outbound Logistics]
        UC7[Execute Internal Location Transfers]
        UC8[Perform Inventory Stock Adjustments]
        UC9[Audit Immutable Double-Entry Stock Ledger]
        UC10[Configure System Settings & Password Reset]
    end

    Worker --> UC1
    Worker --> UC2
    Worker --> UC5
    Worker --> UC6
    Worker --> UC7
    Worker --> UC8
    Worker --> UC10

    Manager --> UC1
    Manager --> UC2
    Manager --> UC3
    Manager --> UC4
    Manager --> UC5
    Manager --> UC6
    Manager --> UC7
    Manager --> UC8
    Manager --> UC9
    Manager --> UC10
```

### Role Capabilities Summary

- **Warehouse Manager (`MANAGER`)**: Full system permissions including product creation and metadata updates, warehouse structure configuration, inventory reconciliation approvals, and full access to audit ledgers.
- **Warehouse Worker (`WORKER`)**: Day-to-day operational execution permissions, including receiving incoming receipts, picking and executing delivery orders, transferring items between locations, and requesting inventory count adjustments.

---

## Core Features

1. **Real-Time Analytics & Dashboard**: Instant metrics on total inventory valuation, active SKU counts, low stock alerts, pending receipt orders, and pending delivery orders.
2. **Product & SKU Catalog Management**: Maintain detailed product records with custom category classification, units of measure, minimum reorder thresholds, and pricing attributes.
3. **Multi-Warehouse & Storage Location Hierarchy**: Multi-tenant warehouse capabilities supporting internal storage zones, racks, vendor locations, customer locations, and inventory loss locations.
4. **Receipt Orders (Inbound Logistics)**: Complete workflow for tracking supplier deliveries from draft state to validation and physical stock placement.
5. **Delivery Orders (Outbound Logistics)**: Streamlined customer order fulfillment with automated reservation checking and stock validation prior to dispatch.
6. **Internal Stock Transfers**: Seamless movement of goods across internal storage locations with state transitions (Draft, In-Transit, Completed).
7. **Physical Stock Adjustments**: Comprehensive inventory counting mechanisms with auto-calculated variance handling and stock reconciliation.
8. **Double-Entry Audit Ledger**: Every inventory adjustment, receipt, delivery, or transfer generates an immutable, timestamped record preserving balance trace history.
9. **Authentication & Password Recovery**: Secure authentication layer with BCrypt password hashing, session tokens, and automated email password reset triggers.

---

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.3.4
- **Language**: Java 21
- **Persistence**: Spring Data JPA / Hibernate
- **Database**: PostgreSQL 16 (Runtime) / H2 (Testing)
- **Security & Crypto**: Spring Security Crypto (BCrypt password encoder)
- **Email Notifications**: Spring Boot Mail / JavaMail

### Frontend
- **Framework**: React 18.3
- **Language**: TypeScript 5.6
- **Build Tool**: Vite 5.4
- **HTTP Client**: Axios 1.7
- **Iconography**: Lucide React
- **Routing & State**: React Router DOM 7, Custom React Hooks & Context

### Infrastructure & Operations
- **Containerization**: Docker & Docker Compose
- **Database Container**: PostgreSQL 16 Alpine

---

## Project Directory Structure

```
StockSense/
├── docker-compose.yml              # PostgreSQL database container orchestration
├── README.md                       # Comprehensive system documentation
├── backend/                        # Spring Boot REST API application
│   ├── pom.xml                     # Maven project configuration & dependencies
│   ├── mvnw / mvnw.cmd             # Cross-platform Maven wrappers
│   └── src/
│       ├── main/
│       │   ├── java/com/stocksense/
│       │   │   ├── StockSenseApplication.java
│       │   │   ├── auth/           # Authentication, User entity & Security Interceptors
│       │   │   ├── common/         # Global exception handling & response utilities
│       │   │   ├── config/         # Cors & Web MVC application configurations
│       │   │   ├── dashboard/      # KPI aggregation services & endpoints
│       │   │   ├── inventory/      # Aggregate stock balance models
│       │   │   ├── ledger/         # Immutable audit trail ledger models & logic
│       │   │   ├── operation/      # Stock adjustment operations & variance logic
│       │   │   ├── order/          # Receipt & Delivery order domain workflows
│       │   │   ├── product/        # SKU management & reorder alert engines
│       │   │   ├── transfer/       # Internal location transfer services
│       │   │   └── warehouse/      # Warehouse & location hierarchy domain models
│       │   └── resources/
│       │       └── application.properties # Spring database & mail configurations
│       └── test/                   # Automated unit & integration tests
└── frontend/                       # React + TypeScript single-page application
    ├── package.json                # Frontend package metadata & script definitions
    ├── vite.config.ts              # Vite build tool configuration
    ├── tsconfig.json               # TypeScript compiler flags
    └── src/
        ├── App.tsx                 # Main application root component & session gate
        ├── main.tsx                # Client DOM entrypoint
        ├── api/                    # Modular Axios HTTP client API instances
        ├── components/             # Reusable UI widgets, forms, headers & sidebars
        ├── hooks/                  # Custom React hooks (toast, state, session)
        ├── pages/                  # Page route view components
        ├── styles/                 # Application design tokens & global CSS
        └── types/                  # TypeScript interfaces and domain type definitions
```

---

## Data Integrity & Stock Ledger Model

StockSense enforces financial-grade double-entry principles for stock management:

- **Immutability**: Stock ledger entries cannot be modified or deleted once persisted. Any corrections require an offsetting adjustment entry.
- **Atomic State Modifications**: Inventory balance updating operations execute within transactional boundaries (`@Transactional`). If any step in a transfer or delivery fails, all ledger and balance changes roll back automatically.
- **Traceability**: Every quantity shift records the source location ID, target location ID, reference order ID, operating user, timestamp, and resulting balance state.

---

## Getting Started

### Prerequisites

Ensure you have the following installed on your host system:
- Java Development Kit (JDK) 21 or higher
- Node.js 18+ and npm
- Docker Desktop or Docker Engine with Docker Compose

### Database Setup with Docker

1. Navigate to the project root directory:
   ```bash
   cd StockSense
   ```

2. Start the PostgreSQL database container:
   ```bash
   docker-compose up -d
   ```
   This initializes a PostgreSQL 16 container exposed on port `5432` with database `stocksense`.

### Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Run the Spring Boot application using Maven:
   ```bash
   # On Windows PowerShell / Command Prompt
   .\mvnw.cmd spring-boot:run

   # On Linux / macOS
   ./mvnw spring-boot:run
   ```

3. The REST API backend will start on `http://localhost:8080`.

### Frontend Setup

1. Open a new terminal window and navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install npm dependencies:
   ```bash
   npm install
   ```

3. Start the Vite development server:
   ```bash
   npm run dev
   ```

4. Open your browser and navigate to `http://localhost:5173` to access the application.

---

## REST API Reference

| Endpoint Domain | HTTP Method | Path | Description | Access Level |
| :--- | :--- | :--- | :--- | :--- |
| **Authentication** | POST | `/api/auth/register` | Register new user account | Public |
| **Authentication** | POST | `/api/auth/login` | Authenticate and obtain token | Public |
| **Authentication** | GET | `/api/auth/me` | Fetch authenticated user profile | Authenticated |
| **Dashboard** | GET | `/api/dashboard/kpis` | Retrieve high-level inventory KPIs | Authenticated |
| **Products** | GET | `/api/products` | Search and filter product catalog | Authenticated |
| **Products** | POST | `/api/products` | Create a new SKU / product | Manager |
| **Products** | PUT | `/api/products/{id}` | Update existing product details | Manager |
| **Warehouses** | GET | `/api/warehouses` | List warehouses & sub-locations | Authenticated |
| **Warehouses** | POST | `/api/warehouses` | Create warehouse entry | Manager |
| **Receipts** | GET | `/api/receipts` | List inbound receipt orders | Authenticated |
| **Receipts** | POST | `/api/receipts` | Create inbound receipt draft | Authenticated |
| **Receipts** | POST | `/api/receipts/{id}/validate` | Validate receipt and post stock | Authenticated |
| **Deliveries** | GET | `/api/deliveries` | List outbound delivery orders | Authenticated |
| **Deliveries** | POST | `/api/deliveries/{id}/validate` | Validate delivery and decrement stock | Authenticated |
| **Transfers** | POST | `/api/transfers` | Execute internal stock transfer | Authenticated |
| **Adjustments** | POST | `/api/operations/adjust` | Record stock count adjustment | Authenticated |
| **Ledger** | GET | `/api/ledger` | Query stock movement audit trail | Manager |

---

## Security & Authentication

- **Password Hashing**: User passwords are encrypted using BCrypt standard algorithm before database insertion.
- **Session Protection**: API requests are validated via custom HTTP header token interception (`AuthInterceptor`).
- **Input Sanitization**: Request payloads undergo server-side bean validation (`@Valid`, `@NotNull`, `@Min`) to block malicious payloads.
