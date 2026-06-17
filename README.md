# 💬 RC (Real-Time Chat) Backend

RC is a highly scalable, production-ready real-time chat backend built with **Spring Boot 3**, **WebSockets**, **Redis Pub/Sub**, and **PostgreSQL**.

The platform provides secure authentication, persistent data storage, and lightning-fast message delivery with support for horizontal scaling across multiple server instances.

---

# 🌟 Features

## 🔐 Robust Authentication System

Supports multiple authentication methods:

- Email & Password Login
- JWT-based Authentication
- Google OAuth2 Login
- Email OTP Verification
- Password Reset via OTP
- Brevo SMTP Integration

---

## ⚡ Scalable Real-Time Messaging

WebSocket connections are protected using:

- `JwtHandshakeInterceptor`
- Spring Security
- JWT validation

Message distribution is powered by:

- Redis Pub/Sub
- Multi-instance broadcasting
- Horizontal scaling support

---

## 💬 Advanced Chat Features

### Group Rooms

Generate secure join codes:

```text
XXX-XXX
```

Users can create and join rooms using these codes.

---

### Direct Messages

Supports one-to-one private conversations.

---

### Atomic Message Upvoting

Messages can be upvoted safely using Redis Sets, preventing duplicate votes and race conditions.

---

### Chat History

Uses Redis Sorted Sets (ZSet) for:

- Chronological ordering
- Fast retrieval
- Real-time synchronization

---

## 📖 API Documentation

Complete REST API documentation is provided using:

- Swagger UI
- OpenAPI 3.0

---

# 🛠 Technology Stack

## Backend Framework

- Java 21
- Spring Boot 3.x

---

## Database

### PostgreSQL

Stores:

- Users
- Chat Rooms
- Messages
- Participants

---

## Cache & Message Broker

### Redis

Handles:

- Pub/Sub broadcasting
- Session management
- Message indexing
- Chat history
- Upvotes

---

## Security

- Spring Security
- JWT (io.jsonwebtoken)
- Google Auth Library

---

## API Documentation

- Springdoc OpenAPI
- Swagger UI

---

## Build Tool

- Maven

---

# 📁 Project Structure

```text
src/main/java/org/godn/rc/

├── auth/
│   ├── controller/
│   ├── security/
│   └── service/
│
├── config/
│
├── entity/
│
├── redis/
│   ├── pubsub/
│   └── store/
│
└── websocket/
    ├── dto/
    ├── handlers/
    ├── manager/
    └── router/
```

---

## Authentication Module

```text
auth/
```

Responsible for:

- User registration
- Login
- Profile management
- OTP verification
- Password reset
- Google OAuth

---

## Configuration Module

```text
config/
```

Contains:

- Redis configuration
- Security configuration
- WebSocket configuration
- OpenAPI configuration

---

## Entity Layer

```text
entity/
```

JPA entities:

- User
- ChatRoom
- Message
- ChatParticipants

---

## Redis Layer

```text
redis/
```

Provides:

### Pub/Sub

Multi-instance message broadcasting.

### RedisChatStore

Maintains:

- Hashes
- Sets
- Sorted Sets (ZSet)

for efficient chat operations.

---

## WebSocket Layer

```text
websocket/
```

Responsible for:

- WebSocket handlers
- Routing actions
- DTOs
- Session management

---

# 🚀 Setup & Installation

## Prerequisites

Ensure the following are installed:

### Java 21+

Required for Spring Boot.

---

### PostgreSQL

Default port:

```text
5432
```

---

### Redis

Default port:

```text
6379
```

---

# ⚙ Environment Configuration

Create a `.env` file alongside `pom.xml`.

Populate it using `.env.example`.

---

## Required Variables

| Variable | Description |
|------------|-------------|
| APP_NAME | Application name |
| DB_URL | PostgreSQL JDBC URL |
| DB_USERNAME | Database username |
| DB_PASSWORD | Database password |
| REDIS_HOST | Redis hostname |
| REDIS_PORT | Redis port |
| JWT_SECRET | Base64 encoded JWT signing key |
| GOOGLE_CLIENT_ID | Google OAuth Client ID |
| GOOGLE_CLIENT_SECRET | Google OAuth Client Secret |
| MAIL_USERNAME | SMTP username |
| MAIL_PASSWORD | SMTP password |
| SENDER_EMAIL | Email used to send OTPs |

---

## Example

```env
APP_NAME=GodnRTC

DB_URL=jdbc:postgresql://localhost:5432/rc_db
DB_USERNAME=postgres
DB_PASSWORD=password

REDIS_HOST=localhost
REDIS_PORT=6379

JWT_SECRET=base64_encoded_secret

GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret

MAIL_USERNAME=your_brevo_username
MAIL_PASSWORD=your_brevo_password
SENDER_EMAIL=noreply@example.com
```

---

# ▶ Build and Run

Using the Maven wrapper:

## Compile

```bash
./mvnw clean install -DskipTests
```

---

## Start the Application

```bash
./mvnw spring-boot:run
```

The server starts on:

```text
http://localhost:8080
```

---

# 🔌 REST API Documentation

Swagger UI becomes available after startup.

### URL

```text
http://localhost:8080/swagger-ui/index.html
```

From there you can test:

- Registration
- Login
- Profile APIs
- OTP verification
- Password reset

---

# 🌐 WebSocket Connection

Clients must provide a valid JWT during the handshake.

### Endpoint

```text
ws://localhost:8080/ws/chat?token=YOUR_JWT_TOKEN_HERE
```

---

# 📦 WebSocket Actions

The `ChatWebSocketHandler` routes requests based on the `action` field.

Supported actions:

- GET_CHATS
- CHAT
- UPVOTE
- CREATE_ROOM
- JOIN
- DIRECT_MESSAGE

---

# 🏠 Creating a Group Room

```json
{
  "action": "CREATE_ROOM",
  "roomType": "GROUP",
  "name": "My Awesome Room"
}
```

---

# 💬 Sending a Message

```json
{
  "action": "CHAT",
  "roomId": "ABC-123",
  "message": "Hello everyone!"
}
```

---

# 👍 Upvoting a Message

```json
{
  "action": "UPVOTE",
  "roomId": "ABC-123",
  "chatId": "uuid-of-the-target-message"
}
```

---

# 🧠 Architecture Highlights

## Redis Data Structures

### Hashes

Store:

- Message metadata
- Sender information
- Chat details

---

### Sorted Sets (ZSet)

Maintain:

- Chronological ordering
- Fast chat history retrieval

using timestamps as scores.

---

### Sets

Track:

- Room members
- User participation
- Message upvotes

while ensuring atomic operations.

---

# 🌍 Multi-Instance Ready

RC supports horizontal scaling.

### Instance A

User sends a WebSocket message.

↓

### Redis Topic

Message is published to Redis.

↓

### Instance B

Receives the event via:

```text
RedisMessageListenerContainer
```

↓

### Local Broadcast

Connected WebSocket sessions receive the message instantly.

---

# 🛡 Immutable Message Design

Messages are identified using UUIDs.

Example:

```text
7b49b89c-f2e4-4f31-bff1-6f8cfb7d3d14
```

Actions such as:

- UPVOTE

do not modify the original message.

Instead, votes are stored independently inside Redis Sets.

This approach:

- Prevents race conditions
- Avoids message corruption
- Improves scalability
- Maintains immutability

---

# 📈 Scalability Architecture

```text
                 +----------------------+
                 |     PostgreSQL        |
                 +-----------+----------+
                             |
                             |
                +------------v------------+
                |      Spring Boot         |
                |--------------------------|
                | Authentication           |
                | REST APIs                |
                | WebSocket Handler        |
                | User Manager             |
                +------------+------------+
                             |
                             |
               Publish / Subscribe
                             |
                             v
                +--------------------------+
                |          Redis            |
                |--------------------------|
                | Pub/Sub                   |
                | Hashes                    |
                | Sets                      |
                | Sorted Sets (ZSet)        |
                +------------+-------------+
                             |
          ---------------------------------------
          |                                     |
          v                                     v
+---------------------+           +----------------------+
| Spring Instance A   |           | Spring Instance B    |
| WebSocket Clients   |           | WebSocket Clients    |
+---------------------+           +----------------------+
```

---

# 🚀 Future Enhancements

Potential additions:

- Typing indicators
- Message reactions
- Read receipts
- Presence tracking
- File uploads
- Media sharing
- Voice channels
- Push notifications
- Kafka integration
- End-to-end encryption

---

# 🎯 Vision

RC aims to provide a robust and scalable foundation for building modern chat applications with:

- Secure authentication
- Real-time communication
- Horizontal scalability
- Persistent storage
- High throughput
- Low latency

making it suitable for production-grade messaging systems.

---

## License

MIT License

---

# 💬 RC — Real-Time Conversations at Scale.
