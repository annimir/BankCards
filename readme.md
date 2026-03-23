# 💳 Bank Cards Management System

REST API для управления банковскими картами.
Java 17 · Spring Boot 3 · PostgreSQL · JWT + Refresh Token · Liquibase · Docker

---

## 🚀 Быстрый старт (Docker Compose)

```bash
git clone <your-repo-url>
cd BankCards
docker-compose up --build
```

Приложение: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
Health check: `http://localhost:8080/actuator/health`

---

## 🛠 Запуск локально (без Docker)

### Требования

- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### 1. Создайте БД

```sql
CREATE DATABASE bankcards;
```

### 2. Переменные окружения

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
export CARD_ENCRYPTION_SECRET=MySecretKey12345MySecretKey12345
```

### 3. Сборка и запуск

```bash
mvn clean package -DskipTests
java -jar target/bankcards-0.0.1-SNAPSHOT.jar
```

Миграции Liquibase применяются автоматически при старте.

---

## 🔐 Аутентификация

| Токен         | Срок жизни | Назначение                             |
|---------------|------------|----------------------------------------|
| Access Token  | 24 часа    | Передаётся в заголовке `Authorization` |
| Refresh Token | 7 дней     | Обменивается на новую пару токенов     |

Все защищённые endpoints требуют заголовок:

```
Authorization: Bearer <ACCESS_TOKEN>
```

При истечении access token используйте `/api/auth/refresh` — старый refresh token
**немедленно инвалидируется** (rotation стратегия).

### Дефолтный администратор (создаётся миграцией)

| Поле     | Значение     |
|----------|--------------|
| username | `admin`      |
| password | `Admin1234!` |

---

## 📋 API Endpoints

### Auth

| Method | URL                   | Описание                                              | Auth   |
|--------|-----------------------|-------------------------------------------------------|--------|
| POST   | `/api/auth/register`  | Регистрация — возвращает оба токена                   | —      |
| POST   | `/api/auth/login`     | Логин — возвращает оба токена                         | —      |
| POST   | `/api/auth/refresh`   | Ротация токенов: старый отзывается, выдаётся новая пара | —    |
| POST   | `/api/auth/logout`    | Отзыв всех refresh токенов пользователя               | Bearer |

### User (роль USER)

| Method | URL                            | Описание                              |
|--------|--------------------------------|---------------------------------------|
| GET    | `/api/user/me`                 | Профиль текущего пользователя         |
| PUT    | `/api/user/me`                 | Обновить email / пароль               |
| GET    | `/api/user/cards`              | Мои карты (фильтр + пагинация)        |
| GET    | `/api/user/cards/{id}`         | Одна карта по ID                      |
| POST   | `/api/user/cards/{id}/block`   | Запросить блокировку своей карты      |
| POST   | `/api/user/cards/transfer`     | Перевод между своими картами          |
| GET    | `/api/user/transfers`          | История всех моих переводов           |
| GET    | `/api/user/cards/{id}/transfers` | История переводов по конкретной карте |

### Admin — Cards (роль ADMIN)

| Method | URL                              | Описание                       |
|--------|----------------------------------|--------------------------------|
| POST   | `/api/admin/cards`               | Создать карту                  |
| GET    | `/api/admin/cards`               | Все карты (фильтр + пагинация) |
| GET    | `/api/admin/cards/{id}`          | Карта по ID                    |
| PUT    | `/api/admin/cards/{id}/activate` | Активировать карту             |
| PUT    | `/api/admin/cards/{id}/block`    | Заблокировать карту            |
| DELETE | `/api/admin/cards/{id}`          | Удалить карту                  |

### Admin — Users (роль ADMIN)

| Method | URL                             | Описание               |
|--------|---------------------------------|------------------------|
| GET    | `/api/admin/users`              | Все пользователи       |
| GET    | `/api/admin/users/{id}`         | Пользователь по ID     |
| POST   | `/api/admin/users`              | Создать пользователя   |
| DELETE | `/api/admin/users/{id}`         | Удалить пользователя   |
| PUT    | `/api/admin/users/{id}/enable`  | Активировать аккаунт   |
| PUT    | `/api/admin/users/{id}/disable` | Деактивировать аккаунт |

### Monitoring

| URL                     | Описание                                       |
|-------------------------|------------------------------------------------|
| `/actuator/health`      | Статус приложения + статистика карт            |
| `/actuator/info`        | Версия и описание приложения                   |
| `/actuator/metrics`     | Метрики JVM и HTTP (требует аутентификации)    |

---

## 📦 Примеры запросов

### Регистрация

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@mail.com","password":"Password1!"}'
```

Ответ (RFC 7807 для ошибок, стандартный JSON для успеха):

```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john",
  "role": "USER"
}
```

### Обновление токена

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"550e8400-e29b-41d4-a716-446655440000"}'
```

### Перевод средств (user)

```bash
curl -X POST http://localhost:8080/api/user/cards/transfer \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"fromCardId":1,"toCardId":2,"amount":500.00}'
```

### История переводов

```bash
curl "http://localhost:8080/api/user/transfers?page=0&size=20" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

### Health check

```bash
curl http://localhost:8080/actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "cardSystem": {
      "status": "UP",
      "details": {
        "cards.total": 150,
        "cards.active": 142,
        "cards.expired": 8,
        "tokens.stale": 0
      }
    },
    "db": { "status": "UP" }
  }
}
```

---

## 🏗 Архитектура

```
src/main/java/com/example/bankcards/
├── config/          # SecurityConfig, OpenApiConfig, AuditConfig
├── controller/      # AuthController, AdminCardController,
│                    # AdminUserController, UserCardController
├── dto/             # AuthDto, CardDto, UserDto, TransferHistoryDto
├── entity/          # User, Card, RefreshToken, TransferHistory,
│                    # Role, CardStatus
├── event/           # TransferCompletedEvent, TransferEventListener
├── exception/       # GlobalExceptionHandler (RFC 7807 ProblemDetail)
├── health/          # CardSystemHealthIndicator
├── mapper/          # CardMapper, UserMapper (MapStruct)
├── repository/      # CardRepository, UserRepository,
│                    # RefreshTokenRepository, TransferHistoryRepository,
│                    # CardSpecification
├── security/        # JwtAuthenticationFilter (MDC), RateLimitingFilter
├── service/         # AuthService, CardService, UserService,
│                    # RefreshTokenService, TransferHistoryService,
│                    # CardExpiryScheduler, UserDetailsServiceImpl
├── util/            # JwtUtil, CardEncryptionUtil
└── validation/      # @ValidCardNumber, CardNumberValidator (Luhn)
```

---

## 🔒 Безопасность

- Номера карт **шифруются AES** в БД — в ответах только маска `**** **** **** 1234`
- Генерируемые номера проходят **алгоритм Луна** (контрольная цифра вычисляется)
- Пароли хэшируются **BCrypt**
- **Access Token** (JWT HS256) — срок жизни 24 часа
- **Refresh Token** (UUID) — срок жизни 7 дней, rotation при обновлении
- **Rate Limiting** (Bucket4j) — 10 запросов/мин на IP для `/login` и `/register`
- **Optimistic Locking** (`@Version`) — защита от race condition при параллельных переводах
- Ролевой доступ через **Spring Security** + `@PreAuthorize`
- **MDC трассировка** — каждый запрос имеет уникальный `traceId` в логах и заголовке `X-Trace-Id`

---

## 📊 Ошибки (RFC 7807 ProblemDetail)

Все ошибки возвращаются в стандартном формате RFC 7807:

```json
{
  "type": "https://bankcards.example.com/errors/insufficient-funds",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "Insufficient funds. Available: 100.00, requested: 500.00"
}
```

Для ошибок валидации добавляется поле `errors`:

```json
{
  "type": "https://bankcards.example.com/errors/validation-error",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "errors": {
    "username": "Username is required",
    "email": "Invalid email format"
  }
}
```

---

## ⏰ Фоновые задачи (`@Scheduled`)

| Задача | Расписание | Описание |
|--------|-----------|----------|
| `CardExpiryScheduler#expireOutdatedCards` | 01:00 ежедневно | ACTIVE карты с истёкшим `expiryDate` → EXPIRED |
| `CardExpiryScheduler#expireOutdatedBlockedCards` | 01:05 ежедневно | BLOCKED карты с истёкшим `expiryDate` → EXPIRED |
| `RefreshTokenService#cleanupExpiredTokens` | 03:00 ежедневно | Удаляет протухшие refresh токены из БД |

---

## 🔍 Фильтрация (JPA Specification)

| Параметр       | Тип          | Применимость |
|----------------|--------------|--------------|
| `status`       | `CardStatus` | admin + user |
| `ownerId`      | `Long`       | только admin |
| `maskedNumber` | `String`     | только user  |

---

## 📋 Аудит

Поля `createdBy`, `lastModifiedBy`, `lastModifiedAt` заполняются автоматически через Spring Data Auditing (`@EnableJpaAuditing`). `AuditorAware` берёт username из `SecurityContext` — при системных операциях используется значение `"system"`.

---

## 🧪 Тесты

```bash
# Все тесты (unit + интеграционные)
mvn test

# Только unit-тесты (без Docker)
mvn test -Dgroups="!integration"
```

> Интеграционные тесты требуют запущенный Docker — Testcontainers поднимает
> PostgreSQL автоматически.

| Класс | Тип | Что покрывает |
|-------|-----|---------------|
| `CardServiceTest` | Unit | Создание, блокировка, активация, переводы |
| `AuthServiceTest` | Unit | Регистрация, логин, refresh, logout |
| `RefreshTokenServiceTest` | Unit | Создание, валидация, ротация, очистка |
| `CardExpirySchedulerTest` | Unit | Джоб истечения карт |
| `CardSpecificationTest` | Unit | Сборка спецификаций, null-параметры |
| `CardEncryptionUtilTest` | Unit | AES, маскирование, Luhn на генерации |
| `CardNumberValidatorTest` | Unit | Алгоритм Луна: валидные/невалидные номера |
| `AuthControllerTest` | Unit | HTTP-слой: статусы, валидация, JSON |
| `CardServiceIntegrationTest` | Integration | Полный цикл с реальным PostgreSQL |
| `AuthIntegrationTest` | Integration | E2E: register → login → refresh → rotation |

---

## 🗄 Миграции Liquibase

| Файл                            | Описание                                      |
|---------------------------------|-----------------------------------------------|
| `001-create-users.xml`          | Таблица `users`                               |
| `002-create-cards.xml`          | Таблица `cards` + индексы                     |
| `003-insert-admin.xml`          | Дефолтный администратор                       |
| `004-create-refresh-tokens.xml` | Таблица `refresh_tokens` + индексы            |
| `005-add-card-version.xml`      | Колонка `version` для optimistic locking      |
| `006-add-audit-fields.xml`      | Колонки `created_by`, `last_modified_by/at`   |
| `007-create-transfer-history.xml` | Таблица `transfer_history` + индексы        |

---

## ⚙️ Переменные окружения

| Переменная               | По умолчанию             | Описание                   |
|--------------------------|--------------------------|----------------------------|
| `DB_USERNAME`            | `postgres`               | Пользователь БД            |
| `DB_PASSWORD`            | `postgres`               | Пароль БД                  |
| `JWT_SECRET`             | `404E635266...` (Base64) | Секрет для подписи JWT     |
| `CARD_ENCRYPTION_SECRET` | `MySecretKey12345...`    | Ключ AES шифрования (32 б) |

> ⚠️ В продакшене обязательно смените все секреты!

---

## 📄 OpenAPI / Swagger

- UI: `http://localhost:8080/swagger-ui.html`
- JSON: `http://localhost:8080/v3/api-docs`
- YAML: `docs/openapi.yaml`