# Nihongo IT - Microservice Architecture

![Backend Tests](https://img.shields.io/badge/backend%20tests-passing-brightgreen)
![Frontend Tests](https://img.shields.io/badge/frontend%20tests-vitest-blue)
![Python Tests](https://img.shields.io/badge/python%20tests-pytest-yellow)

Nền tảng học tiếng Nhật IT xây dựng theo kiến trúc microservice.

## Architecture Overview

```
Frontend (Vue.js :5173)
        │
        ▼
  API Gateway (:8080)          ← JWT validation, Rate limiting, Correlation ID
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
| `api-gateway` | 8080 | Entry point, JWT validation, rate limiting, routing |
| `user-service` | 8086 | Auth (login/register/OAuth2), profile, refresh token |
| `learning-service` | 8088 | Flashcard, FSRS spaced repetition, conversation |
| `ai-service` | 8087 | OpenAI chat, text-to-speech |
| `notification-service` | 8089 | Email notifications, in-app notification REST API |
| `python` | 8000 | FastAPI — NLP (SudachiPy), speech analysis |
| `eureka-server` | 8761 | Netflix Eureka service discovery |

## Technology Stack

**Backend**
- Kotlin + Spring Boot 3.4
- Spring Cloud Gateway + Netflix Eureka + Feign
- Spring Security — JWT (access 2h, refresh 14d với rotation)
- PostgreSQL 16 + Flyway migrations + HikariCP
- Gradle Kotlin DSL với shared `common` module

**Frontend**
- Vue 3 + TypeScript + Vite
- Pinia (state management) + Vue Router
- Vuetify 3

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
├── frontend/                    # Vue 3 + TypeScript
├── python/                      # FastAPI NLP/Speech service
│   ├── config.py
│   ├── nlp.py
│   ├── openai_client.py
│   ├── text_comparison.py
│   ├── speech.py
│   └── main.py
├── docker/                      # Docker Compose + observability config
│   ├── docker-compose.yaml
│   ├── prometheus.yml
│   ├── loki-config.yml
│   ├── promtail-config.yml
│   └── grafana-provisioning/
├── deploy/                      # GCP deploy scripts
├── ddl/                         # SQL schema reference
└── .env                         # Environment variables (không commit)
```

## Setup

### Prerequisites
- Docker Desktop
- Node.js 20+ (frontend)
- JDK 21 (nếu build local)

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

### 3. Chạy frontend local

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
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
| `APP_FRONTEND_URL` | URL frontend (default: `http://localhost:5173`) |
| `CORS_ALLOWED_ORIGINS` | CORS whitelist (default: `http://localhost:5173,http://localhost:3000`) |
| `INTERNAL_API_KEY` | Key bảo vệ Python service |
| `PYTHON_SERVICE_URL` | URL Python service (default: `http://python:8000`) |

## License

[MIT License](LICENSE)
