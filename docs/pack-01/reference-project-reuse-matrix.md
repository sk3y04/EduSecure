# Christialattion Reuse Matrix

This document records what may be taken from `Christialattion/` and what must remain unique to EduSecure.

## Reuse rule

Use the reference project for **structure and technique**, not for domain copying.

## 1. Backend reuse assessment

| Reference item | Relevance | Decision | How to use it |
|---|---|---|---|
| `backend/EmployeeManager/pom.xml` dependency direction | High | Adapt | Prefer a simplified Spring stack: REST, Security, Validation, JPA, PostgreSQL |
| `config/SecurityConfig.java` | High | Adapt | Good starting pattern for authentication, authorization, stateless API security, and password encoding |
| `service/AuthService.java` | High | Adapt | Good separation of login/register logic from controllers |
| `security/CustomUserDetailsService.java` | High | Adapt | Useful Spring Security pattern for loading application users |
| `security/JwtAuthenticationFilter.java` | Medium-High | Adapt carefully | Useful if EduSecure uses JWT for REST sessions; document that JWT is signed token handling, not encryption |
| `controller/AuthController.java` | High | Adapt | Good auth endpoint structure and request validation pattern |
| DTO validation classes such as `LoginRequest.java` and `RegisterRequest.java` | High | Adapt | Reuse validation style, not field/domain definitions |
| `config/DataInitializer.java` | Low-Medium | Use cautiously | Acceptable only for dev/demo bootstrap; not part of final security narrative |
| Repository interfaces such as `UserRepository.java` | Medium | Adapt | Good JPA repository style; rename for EduSecure domain |
| Employee/project/accommodation/timesheet entities and services | Low | Avoid | Wrong domain and would weaken originality |
| Runtime exception style without dedicated error model | Low | Avoid | EduSecure should use clearer, reportable error handling later |

## 2. Frontend reuse assessment

| Reference item | Relevance | Decision | How to use it |
|---|---|---|---|
| `frontend/src/services/api.ts` | High | Adapt | Good axios client pattern, auth header interceptor, error handling basis |
| `frontend/src/stores/auth.ts` | High | Adapt | Good Pinia auth-state pattern; remove dev bypasses in real flow |
| `frontend/src/router/index.ts` | High | Adapt | Good role-based route guard structure for Student, Lecturer, Admin |
| Mock data usage and `DEV_MODE = true` | Low-Medium | Temporary only | Fine during UI prototyping, but not part of the security demonstration |
| Admin/employee page structure | Low | Avoid as domain template | EduSecure needs course, submission, grade, and audit views instead |

## 3. Deployment/devops reuse assessment

| Reference item | Relevance | Decision | How to use it |
|---|---|---|---|
| `Christialattion/docker-compose.yml` | High | Adapt | Good local multi-service pattern for database, backend, frontend |
| `backend/EmployeeManager/Dockerfile` | Medium-High | Adapt | Useful Docker layering/caching approach for backend development |
| env-based application configuration | High | Adopt | Good study-project practice for DB and token settings |

## 4. What EduSecure should definitely take

1. layered backend architecture: controller -> service -> repository -> entity/dto
2. password hashing approach with Spring Security password encoder
3. validation-first DTO style with `@Valid`
4. local Docker and PostgreSQL development patterns
5. frontend auth state, axios interceptor, and route-guard ideas

## 5. What EduSecure should not take

1. employee-management business model
2. entity naming, endpoint naming, and business logic
3. report wording or assignment interpretation
4. development shortcuts as if they were final security controls
5. any implication that JWT replaces TLS or encrypts sensitive academic data

## 6. Immediate conclusion

`Christialattion/` is a useful **engineering reference**, especially for auth flow, layered structure, validation, and local environment setup. It should not be treated as a direct template for the EduSecure domain model or the report narrative.

