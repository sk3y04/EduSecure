# EduSecure

## Introduction
EduSecure is a secure, full-stack educational platform built with a heavy emphasis on data security, academic integrity, and auditability. Developed to meet strict security requirements, it safeguards sensitive academic data by providing end-to-end encrypted communications, highly protected grade management, and robust submission auditing systems.

## Key Features
* **Authentication & Authorization:** Secure, role-based login and access control to ensure users only see what they are authorized to access.
* **Academic Workflows:** Comprehensive tools for continuous attendance tracking, secure assignment submissions, and consistent grading.
* **Security & Integrity:**
  * **Audit Logging:** Immutable submission and grade audit logging to prevent and detect state tampering.
  * **Space Chat:** Integrated end-to-end encrypted (E2EE) messaging for highly secure student-teacher or peer-to-peer interaction.
  * **Operational Risk Management:** Extensive risk mitigations baked into the planning and architecture phases (e.g., CVSS risk registers, CIA evaluation, manual security playbooks).

## Architecture & Tech Stack

### Backend
* **Language & Framework:** Java, Spring Boot
* **Build Tool:** Gradle
* **Database & Migrations:** PostgreSQL, Liquibase

### Frontend
* **Language & Framework:** Vue.js, TypeScript, Vite
* **Styling:** Tailwind CSS

### Infrastructure & Deployment
* **Containerization:** Docker & Docker Compose
* **Web Server / Reverse Proxy:** Nginx
* **NoSQL Datastore:** MongoDB (for specific operational or document structures)

## Project Structure
```text
EduSecure/
├── backend/       # Java/Spring Boot/Gradle backend application and APIs
├── deploy/        # Docker Compose configurations (home-server, VPS) and reverse proxy setups
├── docs/          # Extensive documentation (governance, risk, architecture, crypto setups, feature designs)
├── frontend/      # Vue.js/Vite frontend application (TypeScript, Tailwind CSS)
└── scripts/       # Automation scripts (add-dev-admin, generate production secrets/keys, etc.)
```
## License
Distributed under the MIT License. See `LICENSE` for more information.




