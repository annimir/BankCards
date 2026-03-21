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

Система использует два типа токенов:

| Токен         | Срок жизни | Назначение                              |
|---------------|------------|-----------------------------------------|
| Access Token  | 24 часа    | Передаётся в заголовке `Authorization`  |
| Refresh Token | 7 дней     | Обменивается на новую пару токенов      |

Все защищённые endpoints требуют заголовок:

```
Authorization: Bearer <ACCESS_TOKEN>
```

При истечении access token используйте `/api/auth/refresh` — старый refresh token
при этом **немедленно инвалидируется** (rotation стратегия).

### Дефолтный администратор (создаётся миграцией)

| Поле     | Значение     |
|----------|--------------|
| username | `admin`      |
| password | `Admin1234!` |

---

## 📋 API Endpoints

### Auth

| Method | URL                   | Описание                                    | Auth      |
|--------|-----------------------|---------------------------------------------|-----------|
| POST   | `/api/auth/register`  | Регистрация — возвращает оба токена         | —         |
| POST   | `/api/auth/login`     | Логин — возвращает оба токена               | —         |
| POST   | `/api/auth/refresh`   | Ротация токенов: старый отзывается, выдаётся новая пара | —  |
| POST   | `/api/auth/logout`    | Отзыв всех refresh токенов пользователя     | Bearer    |

### User (роль USER)

| Method | URL                          | Описание                         |
|--------|------------------------------|----------------------------------|
| GET    | `/api/user/me`               | Профиль текущего пользователя    |
| PUT    | `/api/user/me`               | Обновить email / пароль          |
| GET    | `/api/user/cards`            | Мои карты (фильтр + пагинация)   |
| GET    | `/api/user/cards/{id}`       | Одна карта по ID                 |
| POST   | `/api/user/cards/{id}/block` | Запросить блокировку своей карты |
| POST   | `/api/user/cards/transfer`   | Перевод между своими картами     |

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

---

## 📦 Примеры запросов

### Регистрация

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@mail.com","password":"Password1!"}'
```

Ответ:

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

### Создание карты (admin)

```bash
curl -X POST http://localhost:8080/api/admin/cards \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"ownerId":2,"expiryDate":"2028-12-31","initialBalance":1000.00}'
```

### Перевод средств (user)

```bash
curl -X POST http://localhost:8080/api/user/cards/transfer \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"fromCardId":1,"toCardId":2,"amount":500.00}'
```

### Фильтрация карт с пагинацией

```bash
# Мои активные карты, вторая страница
curl "http://localhost:8080/api/user/cards?status=ACTIVE&page=1&size=5&sort=createdAt,desc" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"

# Все карты конкретного пользователя (admin)
curl "http://localhost:8080/api/admin/cards?ownerId=2&status=BLOCKED" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

## 🏗 Архитектура

```
src/main/java/com/example/bankcards/
├── config/          # SecurityConfig, OpenApiConfig
├── controller/      # AuthController, AdminCardController,
│                    # AdminUserController, UserCardController
├── dto/             # AuthDto, CardDto, UserDto
├── entity/          # User, Card, RefreshToken, Role, CardStatus
├── exception/       # GlobalExceptionHandler + кастомные исключения
├── repository/      # CardRepository, UserRepository,
│                    # RefreshTokenRepository, CardSpecification
├── security/        # JwtAuthenticationFilter
├── service/         # AuthService, CardService, UserService,
│                    # RefreshTokenService, CardExpiryScheduler,
│                    # UserDetailsServiceImpl
└── util/            # JwtUtil, CardEncryptionUtil
```

---

## 🔒 Безопасность

- Номера карт **шифруются AES** в БД — в ответах только маска `**** **** **** 1234`
- Пароли хэшируются **BCrypt**
- **Access Token** (JWT HS256) — срок жизни 24 часа
- **Refresh Token** (UUID) — срок жизни 7 дней, хранится в БД, rotation при обновлении
- Ролевой доступ через **Spring Security** + `@PreAuthorize`
- Пользователь видит **только свои карты**

---

## ⏰ Фоновые задачи (`@Scheduled`)

| Задача | Расписание | Описание |
|--------|-----------|----------|
| `CardExpiryScheduler#expireOutdatedCards` | `0 0 1 * * *` (01:00) | Переводит ACTIVE карты с истёкшим `expiryDate` в статус EXPIRED |
| `CardExpiryScheduler#expireOutdatedBlockedCards` | `0 5 1 * * *` (01:05) | То же для BLOCKED карт |
| `RefreshTokenService#cleanupExpiredTokens` | `0 0 3 * * *` (03:00) | Удаляет из БД истёкшие и отозванные refresh токены |

---

## 🔍 Фильтрация (JPA Specification)

Фильтрация карт построена через `CardSpecification` (паттерн Specification из Spring Data JPA).
Каждый параметр независим — `null` значение игнорируется, условие не добавляется в запрос.

| Параметр       | Тип          | Применимость     |
|----------------|--------------|------------------|
| `status`       | `CardStatus` | admin + user     |
| `ownerId`      | `Long`       | только admin     |
| `maskedNumber` | `String`     | только user      |

---

## 🧪 Тесты

```bash
mvn test
```

| Класс | Что покрывает |
|-------|---------------|
| `CardServiceTest` | Создание карты, блокировка, активация, переводы (10 кейсов) |
| `AuthServiceTest` | Регистрация, логин, refresh, logout, дубликаты (8 кейсов) |
| `RefreshTokenServiceTest` | Создание, валидация, ротация, очистка токенов (6 кейсов) |
| `CardExpirySchedulerTest` | Джоб истечения ACTIVE и BLOCKED карт (3 кейса) |
| `CardSpecificationTest` | Сборка спецификаций, null-параметры (6 кейсов) |
| `AuthControllerTest` | HTTP-слой: register, login (3 кейса) |
| `CardEncryptionUtilTest` | AES encrypt/decrypt, маскирование, генерация (4 кейса) |

---

## 🗄 Миграции Liquibase

| Файл                           | Описание                           |
|--------------------------------|------------------------------------|
| `001-create-users.xml`         | Таблица `users`                    |
| `002-create-cards.xml`         | Таблица `cards` + индексы          |
| `003-insert-admin.xml`         | Дефолтный администратор            |
| `004-create-refresh-tokens.xml`| Таблица `refresh_tokens` + индексы |

---

## ⚙️ Переменные окружения

| Переменная               | По умолчанию              | Описание                    |
|--------------------------|---------------------------|-----------------------------|
| `DB_USERNAME`            | `postgres`                | Пользователь БД             |
| `DB_PASSWORD`            | `postgres`                | Пароль БД                   |
| `JWT_SECRET`             | `404E635266...` (Base64)  | Секрет для подписи JWT      |
| `CARD_ENCRYPTION_SECRET` | `MySecretKey12345...`     | Ключ AES шифрования (32 б)  |

> ⚠️ В продакшене обязательно смените все секреты на криптографически стойкие значения!

---

## 📄 OpenAPI / Swagger

- UI: `http://localhost:8080/swagger-ui.html`
- JSON: `http://localhost:8080/v3/api-docs`
- YAML: `docs/openapi.yaml`