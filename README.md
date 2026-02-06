# NVT Project

A full-stack application with Spring Boot backend, frontend, and supporting infrastructure services.

## Prerequisites

- Docker
- Docker Compose

## Running the Application

Start all services with:

```bash
docker-compose up -d
```

This will start the following services:

| Service    | Port(s)      | Description                          |
|------------|--------------|--------------------------------------|
| nginx      | 80           | Reverse proxy (main entry point)     |
| backend    | 8080         | Spring Boot API                      |
| redis      | 6379         | Caching                              |
| minio      | 9000, 9001   | Object storage (API, Console)        |
| rabbitmq   | 5672, 15672  | Message broker (AMQP, Management UI) |
| influxdb   | 8086         | Time-series database                 |
| fakesmtp   | 8001         | Fake SMTP server for development     |

## Accessing the Application

- **Application**: http://localhost
- **Backend API**: http://localhost:8080
- **MinIO Console**: http://localhost:9001 (user: `minioadmin`, password: `minioadmin`)
- **RabbitMQ Management**: http://localhost:15672 (user: `guest`, password: `guest`)
- **InfluxDB**: http://localhost:8086 (user: `admin`, password: `adminpassword`)

## Stopping the Application

```bash
docker-compose down
```

To also remove volumes (persistent data):

```bash
docker-compose down -v
```

## Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
```
