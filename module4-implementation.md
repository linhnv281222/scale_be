# Module 4: Persistence Implementation

## Overview

Module 4 implements data persistence for the ScaleHub IoT system, handling the storage of measurement events from 300 industrial scales into PostgreSQL database with high throughput (up to 300 records/second).

## Architecture

### Components

1. **PersistenceService** - Core service for database operations
2. **BatchPersistenceService** - Handles batching for performance optimization
3. **DeadLetterService** - Error handling and dead letter file management
4. **CoreProcessor Integration** - Triggers persistence from measurement processing
5. **PersistenceProperties** - Configuration management for batch settings

### Data Flow

```
MeasurementEvent (from Queue)
    ↓
CoreProcessor.processEvents()
    ↓
BatchPersistenceService.addToBatch()
    ↓
Batch Processing (configurable size/timeout)
    ↓
PersistenceService.persistMeasurement()
    ↓
├── ScaleCurrentState (UPSERT)
└── WeighingLog (INSERT)
```

## Database Schema

### Updated Tables

- **scale_current_states**: Real-time state with JSONB data fields
- **weighing_logs**: Historical logs with partitioning by created_at

### Data Format

All data fields (data_1 to data_5) are stored as JSONB:
```json
{"val": "150.5"}
```

## Configuration

### Application Properties

```properties
# Persistence Configuration (Module 4)
persistence.batch.size=50
persistence.batch.timeout-ms=500
persistence.dead-letter.directory=dead-letters

# Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

### Configurable Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `persistence.batch.size` | 50 | Number of events to accumulate before persisting |
| `persistence.batch.timeout-ms` | 500 | Maximum wait time before persisting a batch (ms) |
| `persistence.dead-letter.directory` | dead-letters | Directory for failed event logs |

## Performance Optimizations

### Connection Pooling
- HikariCP with max pool size: 20
- Minimum idle: 5
- Optimized for high concurrency

### Batch Processing
- Configurable batch size (default: 50-100 events)
- Configurable timeout (default: 500ms)
- Asynchronous processing on separate threads
- Reduces database I/O by 80-90%

### Partitioning
- weighing_logs partitioned by date for query performance
- Supports millions of daily records

## Error Handling

### Dead Letter Files
Failed persistence events are written to `dead-letters/` directory:
```
dead-letters/dead-letter-scale-{id}-{timestamp}.json
```

### Logging
- Detailed error logging for troubleshooting
- Performance metrics logging
- Batch processing statistics

## Deployment Steps

### 1. Update Database Schema

Run the schema migration script:

```bash
psql -U postgres -d scalehub_db -f update-schema-module4.sql
```

This will:
- Convert data columns from VARCHAR to JSONB
- Add appropriate comments
- Preserve existing data

### 2. Configure Application

Update `application.properties`:

```properties
# Adjust based on your load
persistence.batch.size=50
persistence.batch.timeout-ms=500

# Ensure adequate connection pool
spring.datasource.hikari.maximum-pool-size=20
```

### 3. Build Application

```bash
mvn clean package -DskipTests
```

### 4. Run Application

```bash
java -jar target/ScaleHubIOT-1.0-SNAPSHOT.jar
```

Or with Maven:

```bash
mvn spring-boot:run
```

## Monitoring

### REST Endpoints

**Get Persistence Metrics:**
```
GET /api/v1/monitoring/persistence/metrics
```

Response:
```json
{
  "success": true,
  "data": {
    "batchQueueSize": 0,
    "totalEventsProcessed": 12450,
    "totalEventsFailed": 3,
    "averageBatchSize": 48.5,
    "averageProcessingTimeMs": 125.3,
    "lastProcessedTimestamp": 1735099200000
  }
}
```

**Get Persistence Health:**
```
GET /api/v1/monitoring/persistence/health
```

Response:
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "batchProcessorRunning": true,
    "databaseConnected": true,
    "timestamp": 1735099200000
  }
}
```

### Key Metrics to Monitor

- **Batch Queue Size**: Number of events waiting to be persisted
- **Persistence Throughput**: Events per second being persisted
- **Batch Processing Latency**: Time taken to persist each batch
- **Dead Letter Count**: Number of failed events
- **Database Connection Pool Usage**: Active/idle connections

### Log Patterns

```
[BATCH] Started batch persistence processing with batch-size=50, timeout=500ms
[BATCH] Persisted 48 events in 132ms
[PERSISTENCE] Successfully persisted measurement for scale 5
[DEAD-LETTER] Wrote failed event for scale 12 to dead-letters/dead-letter-scale-12-2025-12-25-09-15-30.json
```

## Testing

### Unit Tests

Run unit tests:
```bash
mvn test -Dtest=PersistenceServiceImplTest
mvn test -Dtest=BatchPersistenceServiceTest
```

### Integration Tests

Run integration tests:
```bash
mvn test -Dtest=PersistenceIntegrationTest
```

### Load Testing

Simulate high load:
```bash
# Use Apache JMeter or similar tool
# Target: 300 events/second
# Duration: 10 minutes
# Monitor: CPU, memory, disk I/O, database connections
```

## Troubleshooting

### High Batch Queue Size

**Symptom**: `batchQueueSize` metric keeps increasing

**Solutions**:
- Increase `persistence.batch.size`
- Decrease `persistence.batch.timeout-ms`
- Check database performance (slow queries, locks)
- Increase connection pool size

### Dead Letter Files Accumulating

**Symptom**: Many files in `dead-letters/` directory

**Solutions**:
- Check database connectivity
- Review error logs for root cause
- Check disk space
- Verify database schema matches entity definitions

### Database Connection Exhaustion

**Symptom**: Connection timeout errors

**Solutions**:
- Increase `spring.datasource.hikari.maximum-pool-size`
- Check for connection leaks
- Optimize transaction boundaries
- Review long-running queries

### Memory Issues

**Symptom**: OutOfMemoryError or high heap usage

**Solutions**:
- Reduce `persistence.batch.size`
- Increase JVM heap size: `-Xmx2g`
- Check for memory leaks
- Monitor batch queue growth

## Performance Tuning

### For High Throughput (>300 events/sec)

```properties
persistence.batch.size=100
persistence.batch.timeout-ms=250
spring.datasource.hikari.maximum-pool-size=30
```

### For Low Latency (<100ms)

```properties
persistence.batch.size=25
persistence.batch.timeout-ms=100
spring.datasource.hikari.maximum-pool-size=15
```

### For Resource Constrained Environments

```properties
persistence.batch.size=30
persistence.batch.timeout-ms=1000
spring.datasource.hikari.maximum-pool-size=10
```

## Future Enhancements

1. **Retry Mechanism** - Implement exponential backoff for failed operations
2. **Metrics Collection** - Integrate with Micrometer/Prometheus
3. **Data Compression** - Compress historical data for storage efficiency
4. **Archive Strategy** - Move old data to cold storage (S3, archive tables)
5. **Batch Optimization** - Implement true batch INSERT statements
6. **Async Replication** - Support for read replicas
7. **Circuit Breaker** - Prevent cascade failures during DB outages