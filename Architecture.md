# ProblemMonitoring Architecture

**ProblemMonitoring** is a distributed platform for real-time monitoring of bank clients' problems. Below is a PlantUML architecture diagram and a detailed description of each component.

## PlantUML Architecture Diagram

```plantuml
@startuml
!include <C4/C4_Container>
!include <C4/C4_Context>

LAYOUT_WITH_LEFT_LEGEND

title ProblemMonitoring Architecture — Bank Client Problem Monitoring System

Person(client, "Bank Client", "Generates business events (via simulator)")

System_Boundary(platform, "ProblemMonitoring Platform") {

  Boundary(eg, "Event Generator") {
    Container(eventgenerator, "Event Generator", "Java 17, Spring Boot, Kafka Producer", "Generates test business events of two types: AUTO (AnalyticalEvent) and VERIFICATED (BusinessEvent). bootstrapServerConfig is configurable via @Value")
  }

  Boundary(ms, "Monitoring Service") {
    Container(problmonitoring, "Problem Monitoring Service", "Java 17, Spring Boot, Kafka Consumer/Producer", "Consumes BusinessEvent from monitoringserv topic, adds approval metadata (employee, timestamp), and sends AnalyticalEvent to analyticalserv topic")
  }

  Boundary(as, "Analytical System") {
    Container(analyticalsystem, "Analytical System", "Java 17, Spring Boot, JPA, H2", "Consumes AnalyticalEvent batches from analyticalserv topic and persists them to H2 in-memory database")
  }

  Boundary(astreams, "Analytical Streams (Spark)") {
    Container(analyticalstream, "Analytical Event Stream", "Scala, Spark Structured Streaming", "Reads analyticalserv stream and writes to PostgreSQL (JDBC)")
    Container(monitoringstream, "Monitoring Event Stream", "Scala, Spark Structured Streaming", "Reads monitoringserv stream and writes to Delta Lake")
  }
}

Boundary(infra, "Infrastructure Layer") {

  Container(kafka, "Apache Kafka", "3 brokers", "Event bus. Topics: monitoringserv, analyticalserv")

  ContainerDb(postgres, "PostgreSQL", "PostgreSQL + pgAdmin", "Target storage for approved analytical events")

  ContainerDb(delta, "Delta Lake", "Local filesystem (Parquet)", "Raw monitoring events storage")

  ContainerDb(h2, "H2 (in-memory)", "H2 Database", "Analytical System in-memory DB")

  Container(vault, "HashiCorp Vault", "KV Secrets Engine v2", "Centralized secret management (sensitive data only: e.g. techUserName)")

  Container(kafdrop, "Kafdrop", "Web UI", "Kafka monitoring web interface")
}

' Kafka topics
System_Ext(monitoringtopic, "topic: monitoringserv", "Stores BusinessEvent (VERIFICATED)")
System_Ext(analyticaltopic, "topic: analyticalserv", "Stores AnalyticalEvent (AUTO + approved)")

Rel(eventgenerator, kafka, "Sends BusinessEvent -> monitoringserv", "Kafka Producer")
Rel(eventgenerator, kafka, "Sends AnalyticalEvent -> analyticalserv", "Kafka Producer")

Rel(kafka, problmonitoring, "Consumes BusinessEvent from monitoringserv", "Kafka Consumer")
Rel(problmonitoring, kafka, "Sends approved AnalyticalEvent -> analyticalserv", "Kafka Producer (ReplyTo)")

Rel(kafka, analyticalsystem, "Consumes AnalyticalEvent batches from analyticalserv", "Kafka Consumer (batch)")
Rel(analyticalsystem, h2, "save()", "JPA/Hibernate")

Rel(kafka, analyticalstream, "Consumes AnalyticalEvent from analyticalserv", "Kafka Consumer (Spark)")
Rel(analyticalstream, postgres, "append", "JDBC (foreachBatch)")

Rel(kafka, monitoringstream, "Consumes BusinessEvent from monitoringserv", "Kafka Consumer (Spark)")
Rel(monitoringstream, delta, "append", "Delta Lake format")

Rel(vault, analyticalsystem, "techUserName", "Spring Cloud Vault")

Rel(kafdrop, kafka, "Topic monitoring, message browsing", "HTTP")

@enduml
```

## Component Descriptions

### 1. Event Generator (`eventgenerator`)
- **Technology:** Java 17, Spring Boot, Spring Kafka
- **Purpose:** Test business event generator. Every second creates an event of a random type:
  - `AUTO` (automatic) → `AnalyticalEvent` → topic `analyticalserv`
  - `VERIFICATED` (requires verification) → `BusinessEvent` → topic `monitoringserv`
- **Configuration:** `bootstrapServerConfig` is injected via `@Value` from `application.yml` (no longer hardcoded). Non-sensitive settings (topics, bootstrap servers) are defined locally; only sensitive secrets are stored in Vault.

### 2. Problem Monitoring Service (`problmonitoring`)
- **Technology:** Java 17, Spring Boot, Spring Kafka (Consumer + Producer)
- **Purpose:** Listens to the `monitoringserv` topic, receives `BusinessEvent`, "approves" it (adds employee ID and approval timestamp), and sends the result as `AnalyticalEvent` back to the `analyticalserv` topic (via `@SendTo` / ReplyTo mechanism)
- **Key feature:** Uses the Kafka Request-Reply pattern — the same container serves both consumption and response sending
- **Configuration:** Topics and bootstrap servers configured locally in `application.yml`

### 3. Analytical System (`analyticalsystem`)
- **Technology:** Java 17, Spring Boot, Spring Kafka (batch consumer), JPA/Hibernate, H2 Database
- **Purpose:** Consumes `AnalyticalEvent` from the `analyticalserv` topic in batches (every 20 seconds of idle, concurrency=3) and persists them to the H2 in-memory database
- **Additional info:** Includes a `load_datetime` field — the timestamp of record ingestion. H2 JDBC URL and bootstrap servers are configured locally; `techUserName` comes from Vault.

### 4. Analytical Streams (`analyticalstreams`)
- **Technology:** Scala 2.13, Apache Spark 3.3.2 (Structured Streaming), Spark Kafka integration
- **Two streams:**
  - **AnalyticalEventStream:** Reads the `analyticalserv` topic, parses JSON, converts timestamps, and writes to **PostgreSQL** via JDBC (ProcessingTime trigger 35 seconds, foreachBatch)
  - **MonitoringEventStream:** Reads the `monitoringserv` topic, parses JSON, converts timestamps, and writes to **Delta Lake** (local Parquet files, ProcessingTime trigger 20 seconds)
- **Utilities:** `Utils.scala` — parses `application.yml` for configuration loading (PostgreSQL credentials, file paths)

### 5. Shared Library (`modellibs`)
- **Data models:**
  - `Event` — abstract base class
  - `BusinessEvent` — event requiring verification (typeEvent, kindEvent, idClient, dateCreate)
  - `AnalyticalEvent` — analytical event (additional: approvedBy, approvedDateTime)
  - **Serializers/Deserializers:** Custom Kafka serializers/deserializers using Jackson (ObjectMapper)

### 6. Infrastructure

| Component | Description |
|-----------|-------------|
| **Apache Kafka** (3 brokers) | Event bus. Topics: `monitoringserv` (3 partitions, RF=3), `analyticalserv` (3 partitions, RF=3) |
| **Zookeeper** | Kafka broker coordination |
| **Kafdrop** | Web UI for visual Kafka monitoring |
| **PostgreSQL** | Target analytical events storage (via Spark Streaming) |
| **pgAdmin** | PostgreSQL administration |
| **H2 (in-memory)** | Analytical System database (local, for fast processing) |
| **HashiCorp Vault** | Centralized secret management for sensitive data (KV v2) |

### 7. Configuration & Secret Management

The project uses a **hybrid configuration approach**:

- **Non-sensitive configuration** (topic names, bootstrap servers, database URLs) is defined directly in each microservice's `application.yml`:
  - `eventgenerator/src/main/resources/application.yml` — `topicMonitoring`, `topicAUTO`, `bootstrapServerConfig`
  - `problmonitoring/src/main/resources/application.yml` — `topicMonitoring`, `topicAUTO`, `bootstrapServerConfig`
  - `analyticalsystem/src/main/resources/application.yml` — `topicAUTO`, `bootstrapServerConfig`, `pgAnalyticalUrl`

- **Sensitive secrets** are stored in HashiCorp Vault:
  - Vault starts in dev mode with `root_token = myroot`
  - The `load-secrets.sh` script loads YAML files from `docker/secret_load_dir/` into Vault (KV v2)
  - Currently only `analyticalsystem.yaml` remains with `techUserName`

### 8. Data Flow

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

**Two processing paths:**
1. **AUTO events** → immediately go to `analyticalserv` → consumed by AnalyticalSystem (H2) and AnalyticalEventStream (PostgreSQL)
2. **VERIFICATED events** → go to `monitoringserv` → Problem Monitoring approves → sends to `analyticalserv` → consumed by AnalyticalSystem (H2) and AnalyticalEventStream (PostgreSQL). In parallel, raw BusinessEvents are persisted by MonitoringEventStream to Delta Lake.

### 9. Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Primary language for backend microservices |
| Scala | 2.13 | Language for Spark Streaming applications |
| Spring Boot | 2.6.7 | Microservice framework |
| Spring Cloud | 2021.0.2 | Vault integration, Bootstrap |
| Apache Kafka | latest (Confluent) | Event bus |
| Apache Spark | 3.3.2 | Structured Streaming analytics |
| Delta Lake | 2.3.0 | Storage format for raw events |
| PostgreSQL | latest | Target database |
| H2 | latest | Analytical System in-memory DB |
| HashiCorp Vault | 1.15.0 | Secret management |
| Maven | - | Java module build |
| sbt | - | Scala module build |
| Docker Compose | - | Infrastructure orchestration |