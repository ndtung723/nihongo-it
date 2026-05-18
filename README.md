# Nihongo IT - Microservice Architecture

![Backend Tests](https://img.shields.io/badge/backend%20tests-passing-brightgreen)
![Frontend Tests](https://img.shields.io/badge/frontend%20tests-vitest-blue)
![Python Tests](https://img.shields.io/badge/python%20tests-pytest-yellow)

Nền tảng học tiếng Nhật IT xây dựng theo kiến trúc microservice.

## Architecture Overview

```
Frontend User  (Next.js :3000)        ← user-facing app
Frontend Admin (Next.js :3002 host)   ← admin app
        │
        ▼
  API Gateway (:8080)                ← JWT validation, Rate limiting, Correlation ID, CORS
        │
        ├──▶ User Service (:8086)          ← Auth, Profile, OAuth2 Google
        ├──▶ Learning Service (:8088)      ← Flashcard, FSRS, Conversation
        ├──▶ AI Service (:8087)            ← OpenAI chat, TTS
        ├──▶ Notification Service (:8089)  ← Email, In-app notifications
        └──▶ Python Service (:8000)        ← NLP, Speech analysis (FastAPI)

  Eureka Server (:8761)        ← Service discovery
  PostgreSQL (:5432)           ← Primary database (named volume)
  Prometheus (:9090)           ← Metrics scraping
  Grafana (:3001)              ← Dashboard (metrics + logs)
  Loki (:3100)                 ← Log aggregation
```

## Services

| Service | Port | Mô tả |
|---|---|---|
| `frontend-user` | 3000 | Next.js 16 user app (vocabulary, flashcards, conversation, speech, tools) |
| `frontend-admin` | 3002 host / 3001 container | Next.js 16 admin app (users, content CRUD, statistics) |
| `api-gateway` | 8080 | Entry point, JWT validation, rate limiting, routing, CORS |
| `user-service` | 8086 | Auth (login/register/OAuth2), profile, refresh token |
| `learning-service` | 8088 | Flashcard, FSRS spaced repetition, conversation |
| `ai-service` | 8087 | OpenAI chat, text-to-speech |
| `notification-service` | 8089 | Email notifications, in-app notification REST API |
| `python` | 8000 | FastAPI — NLP (SudachiPy), speech analysis |
| `eureka-server` | 8761 | Netflix Eureka service discovery |

## Technology Stack

**Backend**
- Kotlin 2.3.0 + Spring Boot 4.0.2 + Spring Cloud 2025.1.1 (JDK 25)
- Spring Cloud Gateway + Netflix Eureka + Feign
- Spring Security 7 — JWT (access 2h, refresh 14d với rotation)
- PostgreSQL 16 + Flyway migrations + HikariCP
- Gradle 9.3.0 (Kotlin DSL) với shared `common` module

**Frontend** (two standalone Next.js apps)
- Next.js 16 (App Router) + React 19 + TypeScript 5
- Tailwind CSS 4 + shadcn/ui (Radix primitives)
- Zustand (state) + react-hook-form + zod + sonner (toast)
- chart.js + react-chartjs-2 (statistics), @dnd-kit/sortable (drag-drop), @tanstack/react-table (admin tables)
- @react-oauth/google (Google login), microsoft-cognitiveservices-speech-sdk (speech), native MediaRecorder (audio)
- Vitest + @testing-library/react

**AI / NLP**
- OpenAI API (GPT, TTS)
- FastAPI + SudachiPy (Japanese NLP)
- Speech analysis (pitch, formants)

**Observability**
- Structured JSON logs (Logstash encoder) + Correlation ID xuyên services
- Prometheus metrics + Grafana dashboard
- Loki + Promtail log aggregation

**Security**
- JWT validate tập trung tại Gateway, inject `X-User-Id/Role/Email` header
- Bucket4j rate limiting (login 5/min, AI 30/min, speech 10/min)
- CORS whitelist qua `CORS_ALLOWED_ORIGINS` env var
- Python service protected bằng `X-Internal-Key`
- httpOnly refresh-token cookie + in-memory access token; single-flight refresh + 5xx exponential backoff in frontend axios client

## Directory Structure

```
nihongo-it/
├── services/                    # Backend microservices (Kotlin/Spring Boot)
│   ├── common/                  # Shared module: security, exception, dto, logging, metrics
│   ├── api-gateway/
│   ├── eureka-server/
│   ├── user-service/
│   ├── learning-service/
│   ├── ai-service/
│   └── notification/
├── frontend-user/               # Next.js 16 user app (port 3000)
├── frontend-admin/              # Next.js 16 admin app (port 3001/3002)
├── python/                      # FastAPI NLP/Speech service
├── docker/                      # Docker Compose + observability config
│   ├── docker-compose.yaml
│   ├── prometheus.yml
│   ├── loki-config.yml
│   ├── promtail-config.yml
│   └── grafana-provisioning/
├── deploy/                      # GCP deploy scripts
├── ddl/                         # SQL schema reference
├── docs/superpowers/plans/      # Migration plans + discoveries
└── .env                         # Environment variables (không commit)
```

## Setup

### Prerequisites
- Docker Desktop
- Node.js 24+ (frontend)
- JDK 25 (nếu build backend local)

### 1. Cấu hình environment

```bash
cp docker/.env.example docker/.env
# Điền các giá trị: DB_PASSWORD, JWT_SECRET, OPENAI_API_KEY, MAIL_*, GOOGLE_CLIENT_*
```

### 2. Khởi động toàn bộ bằng Docker

```bash
cd docker
docker compose up -d
```

Flyway sẽ tự động chạy migrations tạo schema và seed data khi services khởi động.

Truy cập:
- User app: http://localhost:3000
- Admin app: http://localhost:3002
- API Gateway: http://localhost:8080

### 3. Chạy frontend local (hot reload)

```bash
cd frontend-user
npm install
npm run dev
# → http://localhost:3000
```

```bash
cd frontend-admin
npm install
npm run dev
# → http://localhost:3001  (dev mode; docker maps to host 3002)
```

### 4. Build backend (không cần chạy, chỉ compile kiểm tra)

```bash
cd services
./gradlew build -x test
```

## API Documentation (Swagger)

| Service | URL |
|---|---|
| User Service | http://localhost:8086/swagger-ui.html |
| Learning Service | http://localhost:8088/swagger-ui.html |
| AI Service | http://localhost:8087/swagger-ui.html |
| Notification Service | http://localhost:8089/swagger-ui.html |

## Monitoring

| Tool | URL |
|---|---|
| Eureka Dashboard | http://localhost:8761 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (admin / admin) |

## Environment Variables

| Variable | Mô tả |
|---|---|
| `JWT_SECRET` | Secret key ký JWT |
| `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL credentials |
| `POSTGRES_DB` | Tên database |
| `OPENAI_API_KEY` | OpenAI API key |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail app password |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | OAuth2 Google |
| `APP_FRONTEND_URL` | URL frontend-user — dùng cho email links (default: `http://localhost:3000`) |
| `CORS_ALLOWED_ORIGINS` | CORS whitelist (default: `http://localhost:3000,http://localhost:3002`) |
| `NEXT_PUBLIC_API_BASE_URL` | Browser-facing gateway URL được bake vào Next.js bundle (default: `http://localhost:8080`) |
| `INTERNAL_API_KEY` | Key bảo vệ Python service |
| `PYTHON_SERVICE_URL` | URL Python service (default: `http://python:8000`) |

## License

[MIT License](LICENSE)
