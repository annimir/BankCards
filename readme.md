# 💳 Bank Cards Management System

REST API для управления банковскими картами. Java 17 + Spring Boot 3 + PostgreSQL + JWT.

---

## 🚀 Быстрый старт (Docker Compose)

```bash
git clone <your-repo-url>
cd BankCards
docker-compose up --build
```

Приложение запустится на `http://localhost:8080`  
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

### 2. Настройте `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bankcards
    username: postgres
    password: your_password
```

Или задайте переменные окружения:

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
export CARD_ENCRYPTION_SECRET=MySecretKey12345MySecretKey12345
```

### 3. Соберите и запустите

```bash
mvn clean package -DskipTests
java -jar target/bankcards-0.0.1-SNAPSHOT.jar
```

Миграции Liquibase применятся автоматически при старте.

---

## 🔐 Аутентификация

Все защищённые endpoints требуют заголовок:

```
Authorization: Bearer <JWT_TOKEN>
```

### Дефолтный администратор (создаётся миграцией)

| Поле     | Значение       |
|----------|----------------|
| username | `admin`        |
| password | `Admin1234!`   |

---

## 📋 API Endpoints

### Auth

| Method | URL                    | Описание             | Auth |
|--------|------------------------|----------------------|------|
| POST   | `/api/auth/register`   | Регистрация USER     | —    |
| POST   | `/api/auth/login`      | Получить JWT токен   | —    |

### User (роль USER)

| Method | URL                         | Описание                            |
|--------|-----------------------------|-------------------------------------|
| GET    | `/api/user/me`              | Профиль текущего пользователя       |
| PUT    | `/api/user/me`              | Обновить профиль                    |
| GET    | `/api/user/cards`           | Мои карты (фильтр + пагинация)      |
| GET    | `/api/user/cards/{id}`      | Конкретная карта                    |
| POST   | `/api/user/cards/{id}/block`| Запрос блокировки карты             |
| POST   | `/api/user/cards/transfer`  | Перевод между своими картами        |

### Admin — Cards (роль ADMIN)

| Method | URL                          | Описание                       |
|--------|------------------------------|--------------------------------|
| POST   | `/api/admin/cards`           | Создать карту                  |
| GET    | `/api/admin/cards`           | Все карты (фильтр + пагинация) |
| GET    | `/api/admin/cards/{id}`      | Карта по ID                    |
| PUT    | `/api/admin/cards/{id}/activate` | Активировать карту         |
| PUT    | `/api/admin/cards/{id}/block`    | Заблокировать карту        |
| DELETE | `/api/admin/cards/{id}`      | Удалить карту                  |

### Admin — Users (роль ADMIN)

| Method | URL                            | Описание                    |
|--------|--------------------------------|-----------------------------|
| GET    | `/api/admin/users`             | Все пользователи            |
| GET    | `/api/admin/users/{id}`        | Пользователь по ID          |
| POST   | `/api/admin/users`             | Создать пользователя        |
| DELETE | `/api/admin/users/{id}`        | Удалить пользователя        |
| PUT    | `/api/admin/users/{id}/enable` | Активировать аккаунт        |
| PUT    | `/api/admin/users/{id}/disable`| Деактивировать аккаунт      |

---

## 📦 Примеры запросов

### Регистрация

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@mail.com","password":"Password1!"}'
```

### Создание карты (admin)

```bash
curl -X POST http://localhost:8080/api/admin/cards \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"ownerId":2,"expiryDate":"2028-12-31","initialBalance":1000.00}'
```

### Перевод средств (user)

```bash
curl -X POST http://localhost:8080/api/user/cards/transfer \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"fromCardId":1,"toCardId":2,"amount":500.00}'
```

---

## 🏗 Архитектура

```
src/main/java/com/example/bankcards/
├── config/          # SecurityConfig, OpenApiConfig
├── controller/      # AuthController, AdminCardController,
│                    # AdminUserController, UserCardController
├── dto/             # AuthDto, CardDto, UserDto
├── entity/          # User, Card, Role, CardStatus
├── exception/       # GlobalExceptionHandler + кастомные исключения
├── repository/      # UserRepository, CardRepository
├── security/        # JwtAuthenticationFilter
├── service/         # AuthService, CardService, UserService,
│                    # UserDetailsServiceImpl
└── util/            # JwtUtil, CardEncryptionUtil
```

---

## 🔒 Безопасность

- Номера карт **шифруются AES** в БД, в ответах возвращается только маска `**** **** **** 1234`
- Пароли хэшируются **BCrypt**
- Токены **JWT** (HS256), срок действия 24 часа
- Ролевой доступ через **Spring Security** (`@PreAuthorize`)
- Пользователь видит **только свои карты**

---

## 🧪 Тесты

```bash
mvn test
```

Покрыты:
- `CardServiceTest` — бизнес-логика (создание, блокировка, активация, переводы)
- `AuthServiceTest` — регистрация, логин, дубликаты
- `AuthControllerTest` — HTTP слой
- `CardEncryptionUtilTest` — шифрование/дешифрование/маскирование

---

## 🗄 Миграции Liquibase

| Файл                        | Описание                  |
|-----------------------------|---------------------------|
| `001-create-users.xml`      | Таблица `users`           |
| `002-create-cards.xml`      | Таблица `cards` + индексы |
| `003-insert-admin.xml`      | Дефолтный администратор   |

---

## ⚙️ Переменные окружения

| Переменная               | По умолчанию                                              | Описание                   |
|--------------------------|-----------------------------------------------------------|----------------------------|
| `DB_USERNAME`            | `postgres`                                                | Пользователь БД            |
| `DB_PASSWORD`            | `postgres`                                                | Пароль БД                  |
| `JWT_SECRET`             | `404E635266...` (Base64)                                  | Секрет для JWT             |
| `CARD_ENCRYPTION_SECRET` | `MySecretKey12345MySecretKey12345`                        | Ключ шифрования AES (32 б) |



## 📄 OpenAPI

Спецификация доступна по адресу:
- UI: `http://localhost:8080/swagger-ui.html`
- JSON: `http://localhost:8080/v3/api-docs`
- YAML файл: `docs/openapi.yaml` (генерируется автоматически при запуске)