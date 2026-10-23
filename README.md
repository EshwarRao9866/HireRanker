# HireRanker — AI-Powered Resume Screening & Candidate Ranking System

HireRanker is a full-stack recruitment platform designed to simplify candidate management, resume screening, and candidate ranking. It aims to help recruiters evaluate candidates more efficiently by combining a modern web interface, backend APIs, resume processing, and AI-assisted assessment.

## 🚀 Project Overview

Traditional recruitment often involves manually reviewing large numbers of resumes, comparing candidate skills with job requirements, and managing applications across different stages.

HireRanker aims to streamline this process through a centralized platform where candidates can manage their profiles and resumes while administrators or recruiters manage job openings, review applications, and evaluate candidate suitability.

## ✨ Key Features

### 👤 User & Authentication Management
- Candidate registration and login.
- Role-based access for administrators and candidates.
- Secure authentication using Spring Security and JWT.
- Email verification and account-related email workflows, where implemented.

### 📄 Resume Management
- Upload and manage candidate resumes.
- Extract resume text for further processing.
- Maintain candidate resume information.
- Support resume analysis and skill identification.

### 🤖 AI-Assisted Resume Screening
- Compare resume information against job requirements.
- Identify relevant skills and qualifications.
- Generate candidate match scores.
- Support candidate ranking based on job-related criteria.

### 💼 Job & Application Management
- Create and manage job openings.
- Maintain candidate applications.
- Track recruitment progress and application status.

### 🎯 Interview Assessment
- Support interview preparation and assessment workflows.
- Evaluate job-related candidate responses where implemented.
- Present assessment results to help recruiters review candidates.

### 📊 Dashboard & Reporting
- Provide a centralized recruiter dashboard.
- Display recruitment information and candidate statistics.
- Support future enhancements for analytics and report generation.

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Angular | Frontend development |
| TypeScript, HTML, CSS | User interface and application logic |
| Java 21 | Backend programming |
| Spring Boot 3.3.x | REST API development |
| Spring Security | Authentication and authorization |
| JWT | Token-based authentication |
| MySQL | Relational database |
| Apache PDFBox | PDF text extraction, where implemented |
| Resend API | Email delivery and verification workflows |
| AI service integrations | AI-assisted screening and assessment, as configured |

## 🏗️ System Architecture

```text
┌──────────────────────────────┐
│       Angular Frontend       │
│  Candidate & Admin Interfaces│
└──────────────┬───────────────┘
               │ REST API / HTTP
               ▼
┌──────────────────────────────┐
│       Spring Boot Backend    │
│                              │
│ Authentication & Authorization│
│ User and Job Management      │
│ Resume Processing            │
│ Screening and Ranking        │
│ Interview Assessment        │
│ Email Integration            │
└───────┬──────────────┬───────┘
        │              │
        ▼              ▼
┌──────────────┐  ┌────────────────┐
│    MySQL     │  │ External Services│
│   Database   │  │ Resend / AI APIs │
└──────────────┘  └────────────────┘
```

## 📂 Project Structure

The following is a conceptual structure. Actual folders and package names may differ depending on the current implementation.

```text
HireRanker/
├── frontend/
│   └── Angular application
├── backend/
│   └── Spring Boot application
├── README.md
└── .gitignore
```

### Backend Modules

- Controllers — REST API endpoints.
- Services — application and business logic.
- Repositories — database access.
- Entities — database models.
- DTOs — request and response objects.
- Security — authentication and authorization.
- Configuration — database, email and external API configuration.

## ⚙️ Prerequisites

Install the following software before running the project:

- Node.js and npm, compatible with your Angular version.
- Angular CLI.
- Java Development Kit (JDK) 21.
- Maven, or use the project's Maven Wrapper if available.
- MySQL Server.
- Git.

External API credentials are required for integrations that depend on live services.

## 🔧 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/EshwarRao9866/HireRanker.git
cd HireRanker
```

Use the actual repository URL if your project is hosted under a different GitHub account.

### 2. Configure MySQL

Create a MySQL database using the name configured in your backend application.

For example:

```sql
CREATE DATABASE hireranker;
```

Configure the database URL, username, and password using your project's existing Spring Boot configuration.

Do not commit database passwords or other secrets to GitHub.

### 3. Configure Backend Environment Variables

Set the environment variables required by your current implementation. Depending on the enabled features, these may include:

```text
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password
RESEND_API_KEY=your_resend_api_key
RESEND_SENDER_EMAIL=your_verified_sender_email
RESEND_SENDER_NAME=HireRanker
FRONTEND_URL=http://localhost:4200
```

Configure any additional AI provider API keys required by your project.

**Important:** Use real credentials from your own accounts. Never place API keys, passwords, JWT secrets, or private tokens in source code or commit them to GitHub. Configure the sender address using a sender permitted by your Resend account.

### 4. Run the Spring Boot Backend

Navigate to the backend directory:

```bash
cd backend
```

Run the application using the Maven Wrapper, if available:

```bash
./mvnw spring-boot:run
```

On Windows Command Prompt:

```bat
mvnw.cmd spring-boot:run
```

If the project does not include a Maven Wrapper, use:

```bash
mvn spring-boot:run
```

The backend address and port depend on your active Spring Boot configuration. If configured for port 8080, the base URL is:

`http://localhost:8080`

### 5. Run the Angular Frontend

Open a new terminal and navigate to the Angular project directory:

```bash
cd frontend
npm install
ng serve
```

If Angular CLI is not installed globally, use:

```bash
npx ng serve
```

If configured with the default development port, open:

`http://localhost:4200`

**Note:** Replace the example `frontend` and `backend` directory names with the actual paths in your repository.

## 🔐 Security Considerations

- Use password hashing rather than storing plaintext passwords.
- Protect backend endpoints with Spring Security and appropriate roles.
- Verify JWTs on protected requests.
- Keep external API keys in environment variables or a secrets manager.
- Restrict candidate data to authorized users.
- Validate uploaded files and limit their size and allowed types.
- Store verification tokens securely, enforce expiration, and prevent token reuse.
- Avoid logging passwords, API keys, and sensitive candidate information.

## 🧪 Testing

Run the tests supported by your project.

Backend:

```bash
mvn test
```

Frontend:

```bash
npm test
```

The exact frontend test command depends on the test runner configured in `package.json`.

Before considering the application production-ready, test authentication, authorization, resume uploads, database persistence, email delivery, AI service failures, and application error handling.

## 🗺️ Future Enhancements

Potential future improvements include:

- Semantic resume-to-job matching.
- Explainable AI scoring and skill-gap analysis.
- Improved PDF parsing and resume version management.
- Automated interview question generation.
- Structured interview assessment reports.
- Recruitment analytics and downloadable reports.
- Interview scheduling and calendar integration.
- Multi-company recruitment support.
- Cloud deployment, monitoring, backups, and CI/CD.
- Accessibility, privacy controls, and candidate feedback mechanisms.

These are planned enhancements unless already implemented in the current codebase.

## 🎯 Project Objective

The primary objective of HireRanker is to create a centralized, secure, and extensible recruitment platform that supports resume screening, candidate evaluation, and recruitment workflow management.

AI-generated scores are intended to support recruiter review rather than replace human judgment. Candidate assessments should be based on relevant job criteria and verifiable evidence.

## 👨‍💻 Contributors

- **G. Eshwar Rao**

## 📄 License

Choose and add an appropriate open-source license before permitting others to reuse or distribute the project. If no license has been added, the repository remains subject to the default copyright rules.

---

**Developed using Angular, Spring Boot, Java, MySQL, and AI-assisted recruitment technologies.**
