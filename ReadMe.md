# Notes API (Spring Boot + MongoDB)

Simple REST API for managing notes (no UI). Notes are stored in MongoDB.

### Features

- Create / update / delete notes
- List notes with pagination (newest first) and optional tag filter
- Get note text via a dedicated endpoint
- Word statistics: unique words with counts, sorted by count (desc)

### Requirements

- Docker + Docker Compose (recommended)
  or
- Java 21 + Gradle

# Run with Docker Compose

Build and run:

```bash
docker compose up --build
```

### Stop

```bash
docker compose down
```

### Remove volumes (wipe MongoDB data)

```bash
docker compose down -v
```

# Run localy

### Start MongoDB

```bash
docker run --name notes-mongo -p 27017:27017 -d mongo:7
```

### Run the app

```bash
./gradlew bootRun
```

# Tests

```bash
./gradlew test
```