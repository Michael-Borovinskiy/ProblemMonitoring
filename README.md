# ProblemMonitoring

**Building microservices around the process of bank's clients problem monitoring**

A distributed platform for real-time monitoring of bank clients' problems. The system is built on a microservice architecture using Apache Kafka as an event bus, Apache Spark Structured Streaming for analytical processing, and HashiCorp Vault for sensitive secret management.

## Architecture

Detailed architecture description, PlantUML diagram, and data flow are presented in the [Architecture.md](./Architecture.md) file.

### Key Components

```
                          ┌──────────────────────────────────────────────┐
                          │              Event Generator                 │
                          │         (every 1 sec, random type)           │
                          └────┬───────────────────────────┬─────────────┘
                               │ AUTO                      │ VERIFICATED
                               ▼                           ▼
                    ┌───────────────────┐       ┌───────────────────┐
                    │  analyticalserv   │       │  monitoringserv   │
                    │ (AnalyticalEvent) │       │  (BusinessEvent)  │
                    └────────┬──────────┘       └─────────┬─────────┘
                             │                            │
              ┌──────────────┼─────────────┐              │
              ▼              ▼             │              ▼
    ┌─────────────┐   ┌───────────┐        │   ┌───────────────────┐
    │ Analytical  │   │Analytical │        │   │ Problem Monitoring│
    │ System (H2) │   │EventStream│        │   │ Service           │
    │             │   │ (Spark →  │        │   │ (approves, sends  │
    │             │   │PostgreSQL)│        │   │ to analyticalserv)│
    └─────────────┘   └───────────┘        │   └─────────┬─────────┘
                                           │             │
                                           │             ▼
                                           │   ┌───────────────────┐
                                           │   │  analyticalserv   │
                                           └───▶ (approved         │
                                               │  AnalyticalEvent) │
                                               └─────────┬─────────┘
                                                         ▼
                                               ┌────────────────────┐
                                               │  MonitoringEvent   │
                                               │  Stream (Spark →   │
                                               │  Delta Lake)       │
                                               └────────────────────┘
```

## Module Composition

### Maven Modules (Java/Spring Boot)

| Module | Path | Description |
|--------|------|-------------|
| `modellibs` | `./modellibs/` | Shared data model library (Event, BusinessEvent, AnalyticalEvent) and Kafka serializers |
| `eventgenerator` | `./eventgenerator/` | Test business events generator (every 1 sec) |
| `problmonitoring` | `./problmonitoring/` | Problem monitoring service — BusinessEvent approval |
| `analyticalsystem` | `./analyticalsystem/` | Analytical system — batch consumption and H2 persistence |

### SBT Module (Scala/Spark)

| Module | Path | Description |
|--------|------|-------------|
| `analyticalstreams` | `./analyticalstreams/` | Spark Structured Streaming applications for analytical processing |

## Technology Stack

| Technology | Version |
|------------|---------|
| Java | 17 |
| Scala | 2.13 |
| Spring Boot | 2.6.7 |
| Spring Cloud | 2021.0.2 |
| Apache Kafka | Confluent (latest) |
| Apache Spark | 3.3.2 |
| Delta Lake | 2.3.0 |
| PostgreSQL | latest |
| H2 Database | latest |
| HashiCorp Vault | 1.15.0 |
| Maven | - |
| sbt | - |
| Docker Compose | - |

## Data Models

### Event (abstract base class)
Base class for all system events.

### BusinessEvent (requires verification)
```json
{
  "typeEvent": "VERIFICATED",
  "kindEvent": "REVENUE_DECREASE | SUPPLIER_LOSS | FINANCIAL_RISKS | TURNOVER_INCREASE | ACCOUNT_LOCK",
  "idClient": "XX",
  "dateCreate": "timestamp"
}
```

### AnalyticalEvent (analytical event)
```json
{
  "typeEvent": "AUTO | VERIFICATED",
  "kindEvent": "LAW_SUITS | BANKRUPTCY | REVENUE_DECREASE | ...",
  "idClient": "XX",
  "dateCreate": "timestamp",
  "approvedBy": "employee XX",
  "approvedDateTime": "timestamp"
}
```

## Kafka Topics

| Topic | Event Type | Partitions | Replication Factor | Description |
|-------|-------------|-----------|--------------------|-------------|
| `monitoringserv` | BusinessEvent | 3 | 3 | Events requiring verification |
| `analyticalserv` | AnalyticalEvent | 3 | 3 | Analytical events (AUTO + approved) |

## Data Stores

| Store | Technology | Purpose |
|-------|------------|---------|
| H2 (in-memory) | `analyticalsystem` | Fast analytical event persistence via JPA |
| PostgreSQL | `analyticalstreams` | Target analytical events storage (via Spark JDBC) |
| Delta Lake | `analyticalstreams` | Raw monitoring events storage (local Parquet) |

## Configuration Management

The project uses a **hybrid configuration approach**:

### Local Configuration (application.yml)
Non-sensitive settings are defined directly in each microservice's `application.yml`:

| Microservice | Keys in `application.yml` |
|-------------|---------------------------|
| `eventgenerator` | `topicMonitoring`, `topicAUTO`, `bootstrapServerConfig` |
| `problmonitoring` | `topicMonitoring`, `topicAUTO`, `bootstrapServerConfig` |
| `analyticalsystem` | `topicAUTO`, `bootstrapServerConfig`, `pgAnalyticalUrl` |

### HashiCorp Vault (sensitive secrets only)
Sensitive data is stored in HashiCorp Vault (KV v2). Vault starts in dev mode with `root_token = myroot`.

### Secret Loading
Automatic loading of YAML files from `docker/secret_load_dir/` into Vault is performed by the `docker/load-secrets.sh` script when the infrastructure starts.

## Infrastructure (Docker Compose)

The main `docker-compose.yml` starts the following services:

| Service | Port | Purpose |
|---------|------|---------|
| `zookeeper` | 2181 | Kafka coordination |
| `kafka-1` | 9092 (ext), 19092 (int) | Broker 1 |
| `kafka-2` | 9093 (ext), 19093 (int) | Broker 2 |
| `kafka-3` | 9094 (ext), 19094 (int) | Broker 3 |
| `init-kafka` | - | Topic initialization |
| `kafdrop` | 9000 | Kafka Web UI |
| `database` (PostgreSQL) | 15432 | Target database |
| `pgadmin` | 15433 | PostgreSQL administration |
| `vault_srvc` | 8200 | HashiCorp Vault |
| `vault-secrets-loader_srvc` | - | Secret loading into Vault |

### Starting Infrastructure

```bash
# 1. Navigate to the docker directory
cd docker

# 2. Set up environment (copy .env.example → .env)
cp .env.example .env
# Edit .env with actual values

# 3. Start all services
docker compose up -d
```

### Environment Variables

| Variable | Description |
|----------|-------------|
| `DOCKER_HOST_IP` | Host IP for Kafka advertised listeners (default 127.0.0.1) |
| `POSTGRES_USER` | PostgreSQL user |
| `POSTGRES_PASSWORD` | PostgreSQL password |
| `PGADMIN_EMAIL` | pgAdmin email |
| `PGADMIN_PASSWORD` | pgAdmin password |
| `VAULT_TOKEN` | Vault token |

## Running Applications

### Prerequisites

- Java 17+
- Scala 2.13 / sbt
- Docker & Docker Compose
- Maven 3.8+

### Startup Sequence

```bash
# 1. Build modellibs (required before all other modules)
mvn clean install -pl modellibs -am

# 2. Start infrastructure (Kafka, Vault, DB)
cd docker && docker compose up -d

# 3. Build and run microservices
# Event Generator (port 8085)
mvn spring-boot:run -pl eventgenerator

# Problem Monitoring Service (default port)
mvn spring-boot:run -pl problmonitoring

# Analytical System (port 8081)
mvn spring-boot:run -pl analyticalsystem

# 4. Spark Streaming applications (from analyticalstreams directory)
cd analyticalstreams
sbt run
# Choose: AnalyticalEventStream or MonitoringEventStream
```

## Data Flow

### AUTO Events Path (automatic)
```
Event Generator ──(AnalyticalEvent)──→ analyticalserv ──→ Analytical System (H2)
                                                        ──→ AnalyticalEventStream → PostgreSQL
```

### VERIFICATED Events Path (require approval)
```
Event Generator ──(BusinessEvent)──→ monitoringserv ──→ Problem Monitoring Service
                                                           │
                                                           ├──→ MonitoringEventStream → Delta Lake
                                                           │
                                                           └──→ analyticalserv (approved AnalyticalEvent)
                                                                   │
                                                                   ├──→ Analytical System (H2)
                                                                   └──→ AnalyticalEventStream → PostgreSQL
```

## Project Structure

```
ProblemMonitoring/
├── pom.xml                          # Parent POM (Maven multi-module)
├── README.md                        # Project documentation
├── Architecture.md                  # Architecture and PlantUML diagram
│
├── modellibs/                       # Shared model library
│   └── src/main/java/ru/boro/busapps/modellibs/
│       ├── models/                  # Event, BusinessEvent, AnalyticalEvent + serializers
│       └── enums/                   # TypeEvent, KindVerificatedType, KindAutoType
│
├── eventgenerator/                  # Event generator
│   └── src/main/java/ru/boro/busapps/eventgenerator/
│       ├── service/                 # GeneratorService, GeneratorServiceImpl
│       └── kafkaconfigs/            # KfkConfig (Kafka Producer, parameterized bootstrapServerConfig)
│
├── problmonitoring/                 # Problem monitoring service
│   └── src/main/java/ru/boro/busapps/problmonitoring/
│       ├── service/consumer/        # ProblMonitoringService, KfkEventListener
│       └── kafkaconfigs/            # KfkConfig (Consumer + Producer)
│
├── analyticalsystem/               # Analytical system
│   └── src/main/java/ru/boro/bussaps/analyticalsystem/
│       ├── service/                 # AnalyticalService (batch consumer)
│       ├── repo/                    # AnalyticalEventRepository
│       ├── entity/                  # AnalyticalEvent (JPA Entity)
│       └── kafkaconfigs/            # KfkConfig (Batch Consumer)
│
├── analyticalstreams/              # Spark Structured Streaming
│   └── src/main/scala/ru/boro/bussaps/analyticalstreams/
│       ├── AnalyticalEventStream.scala   # Spark → PostgreSQL
│       ├── MonitoringEventStream.scala   # Spark → Delta Lake
│       └── Utils.scala                  # Configuration
│
└── docker/                          # Infrastructure
    ├── docker-compose.yml           # Kafka, ZK, Kafdrop, PostgreSQL, pgAdmin, Vault
    ├── .env / .env.example          # Environment variables
    ├── init.sql                     # PostgreSQL initialization
    ├── load-secrets.sh              # Secret loading into Vault
    └── secret_load_dir/             # YAML secret files (only analyticalsystem.yaml active)
        └── analyticalsystem.yaml    # techUserName (sensitive data only)
```

## Development

### Building the Project

```bash
# Full build of all modules
mvn clean install

# Build specific module
mvn clean install -pl modellibs
mvn clean install -pl eventgenerator -am

# Spark module (sbt)
cd analyticalstreams
sbt compile
```

## License

This project is educational/demonstration only.