# HireRanker - AI-Powered Recruitment & Candidate Ranking Platform

HireRanker is a modern recruitment and resume ranking platform featuring an Angular frontend, Spring Boot REST API backend, and MySQL database integration.

---

## Repository Structure

```text
HireRanker/
│
├── src/                          # Angular 19+ Frontend source code
├── public/                       # Angular static public assets
├── angular.json                  # Angular CLI configuration
├── package.json                  # Frontend dependencies & scripts
├── tsconfig.json                 # TypeScript compiler configuration
│
├── hireranker-backend/           # Spring Boot Backend (Java 21, Spring Boot 3)
│   ├── src/
│   │   ├── main/java/spring/eshwar/   # Controllers, Services, Repositories, Entities, DTOs, Security
│   │   └── main/resources/            # application.properties & configuration
│   ├── pom.xml                        # Maven project dependencies & plugins
│   ├── mvnw / mvnw.cmd                # Maven Wrapper scripts
│   └── application.properties.example # Environment variable setup guide
│
├── .gitignore                    # Git ignore rules for Frontend & Backend
└── README.md                     # Platform documentation
```

---

## Prerequisites

- **Java JDK**: Version 21 or higher
- **Node.js**: Version 18.x or 20.x+ & npm
- **Angular CLI**: `npm install -g @angular/cli`
- **MySQL Server**: Version 8.0 or higher

---

## Database Configuration (MySQL)

1. Ensure MySQL server is running on `localhost:3306`.
2. Create the application database (optional, Spring Boot creates it automatically if permissions allow):
   ```sql
   CREATE DATABASE IF NOT EXISTS hireranker;
   ```
3. Set your local MySQL credentials via environment variables:
   - Windows (PowerShell):
     ```powershell
     $env:DB_USERNAME="root"
     $env:DB_PASSWORD="your_mysql_password"
     ```
   - Linux / macOS (Bash):
     ```bash
     export DB_USERNAME=root
     export DB_PASSWORD=your_mysql_password
     ```
   *(Hibernate will automatically manage schema updates via `spring.jpa.hibernate.ddl-auto=update`)*.

---

## Getting Started

### 1. Run the Spring Boot Backend

Navigate to the `hireranker-backend` folder and start the application using the Maven wrapper:

- **Windows**:
  ```powershell
  cd hireranker-backend
  .\mvnw.cmd spring-boot:run
  ```
- **Linux / macOS**:
  ```bash
  cd hireranker-backend
  chmod +x mvnw
  ./mvnw spring-boot:run
  ```

The backend server will start on `http://localhost:8080`.
You can verify health by navigating to: `http://localhost:8080/api/health`.

### 2. Run the Angular Frontend

Open a new terminal at the repository root:

```bash
npm install
npm start
```
*(or `ng serve`)*

Open your browser and navigate to:
**`http://localhost:4200`**

---

## Key Features

- **Candidate Portal**: Profile management, resume upload, live ATS screening feedback, job browsing, and application status tracking.
- **Admin & HR Dashboard**: Real-time recruitment analytics, conversion funnel, applicant screening, shortlisting, and candidate ranking leaderboards.
- **AI-Powered Screening**: Automated resume text extraction and multi-criteria evaluation.
- **Live Interview Scheduling**: Integrated candidate interview planning and automated notifications.
- **JWT Authentication & Security**: Role-based access control (CANDIDATE, ADMIN) with JWT bearer token verification.
