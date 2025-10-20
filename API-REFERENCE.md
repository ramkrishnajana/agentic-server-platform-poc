# API Reference - Calculation Operations

## Quick Reference

The platform provides 4 calculation operations through reactive Spring WebFlux endpoints.

### Endpoint Summary

| Operation | HTTP Method | Endpoint | Language | Plugin |
|-----------|-------------|----------|----------|--------|
| **Add** | POST | `/api/v1/calculate/add` | Java | java-plugin-add |
| **Multiply** | POST | `/api/v1/calculate/multiply` | Java | java-plugin-multiply |
| **Subtract** | POST | `/api/v1/calculate/subtract` | Python | python-plugin-subtract |
| **Divide** | POST | `/api/v1/calculate/divide` | Python | python-plugin-divide |

---

## 1. Add Operation

**Endpoint**: `POST /api/v1/calculate/add`  
**Plugin**: Java (plugins/java-plugins/add)  
**Runtime**: Java Runtime Supervisor (port 9091)

### Request
```bash
curl -X POST http://localhost:8080/api/v1/calculate/add \
  -H "Content-Type: application/json" \
  -d '{"operand1": 10, "operand2": 5}'
```

### Response
```json
{
  "result": 15.0,
  "operation": "add",
  "operand1": 10.0,
  "operand2": 5.0
}
```

---

## 2. Multiply Operation

**Endpoint**: `POST /api/v1/calculate/multiply`  
**Plugin**: Java (plugins/java-plugins/multiply)  
**Runtime**: Java Runtime Supervisor (port 9091)

### Request
```bash
curl -X POST http://localhost:8080/api/v1/calculate/multiply \
  -H "Content-Type: application/json" \
  -d '{"operand1": 10, "operand2": 5}'
```

### Response
```json
{
  "result": 50.0,
  "operation": "multiply",
  "operand1": 10.0,
  "operand2": 5.0
}
```

---

## 3. Subtract Operation

**Endpoint**: `POST /api/v1/calculate/subtract`  
**Plugin**: Python (plugins/python-plugins/subtract)  
**Runtime**: Python Runtime Supervisor (port 9092)

### Request
```bash
curl -X POST http://localhost:8080/api/v1/calculate/subtract \
  -H "Content-Type: application/json" \
  -d '{"operand1": 10, "operand2": 5}'
```

### Response
```json
{
  "result": 5.0,
  "operation": "subtract",
  "operand1": 10.0,
  "operand2": 5.0
}
```

---

## 4. Divide Operation

**Endpoint**: `POST /api/v1/calculate/divide`  
**Plugin**: Python (plugins/python-plugins/divide)  
**Runtime**: Python Runtime Supervisor (port 9092)

### Request
```bash
curl -X POST http://localhost:8080/api/v1/calculate/divide \
  -H "Content-Type: application/json" \
  -d '{"operand1": 10, "operand2": 5}'
```

### Response
```json
{
  "result": 2.0,
  "operation": "divide",
  "operand1": 10.0,
  "operand2": 5.0
}
```

### Error Handling (Division by Zero)
```bash
curl -X POST http://localhost:8080/api/v1/calculate/divide \
  -H "Content-Type: application/json" \
  -d '{"operand1": 10, "operand2": 0}'
```

**Response (Error)**:
```json
{
  "timestamp": "2025-10-20T14:38:44.990+00:00",
  "path": "/api/v1/calculate/divide",
  "status": 500,
  "error": "Internal Server Error",
  "requestId": "..."
}
```

---

## Request/Response Schema

### CalculationRequest
```json
{
  "operand1": double,
  "operand2": double
}
```

**Constraints**:
- Both operands required
- Must be valid numbers
- For divide: operand2 cannot be 0

### CalculationResult (Success)
```json
{
  "result": double,
  "operation": string,
  "operand1": double,
  "operand2": double
}
```

### Error Response
```json
{
  "timestamp": string (ISO 8601),
  "path": string,
  "status": number (500),
  "error": string,
  "requestId": string
}
```

---

## Performance Characteristics

| Aspect | Java Plugins | Python Plugins |
|--------|--------------|----------------|
| **Container Spawn** | ~4 seconds | ~4 seconds |
| **DNS Propagation** | ~1 second | ~1 second |
| **Execution Time** | ~80ms | ~60ms |
| **Total Latency** | ~5.7 seconds | ~5.6 seconds |
| **Memory Usage** | ~100MB | ~50MB |
| **Image Size** | 320MB | 221MB |

**Note**: Timings are for ephemeral worker mode (fresh container per request).

---

## Technology Stack per Plugin

### Java Plugins (Add, Multiply)
- **Framework**: Spring Boot 3.2
- **Protocol**: gRPC (Platform-Plugin Protocol)
- **Server**: Java gRPC with Netty
- **Base Image**: eclipse-temurin:17-jre
- **GraalVM**: Supported via Dockerfile.native

### Python Plugins (Subtract, Divide)
- **Runtime**: Python 3.11
- **Protocol**: gRPC (Platform-Plugin Protocol)
- **Server**: Python gRPC server
- **Base Image**: python:3.11-slim
- **Dependencies**: grpcio 1.60.0, protobuf 4.25.1

---

## Gateway Technology (All Endpoints)

- **Framework**: Spring Boot 3.2 + Spring WebFlux
- **Web Server**: Netty (reactive, non-blocking)
- **Return Type**: `Mono<CalculationResult>` (reactive streams)
- **Concurrency**: Event loop + bounded elastic thread pool
- **Error Handling**: Reactive error propagation via `doOnError()`

---

## Example Usage Scenarios

### Simple Calculation
```bash
# Calculate 100 ÷ 4 = 25
curl -X POST http://localhost:8080/api/v1/calculate/divide \
  -H "Content-Type: application/json" \
  -d '{"operand1": 100, "operand2": 4}'
```

### Decimal Division
```bash
# Calculate 10 ÷ 3 = 3.3333...
curl -X POST http://localhost:8080/api/v1/calculate/divide \
  -H "Content-Type: application/json" \
  -d '{"operand1": 10, "operand2": 3}'

# Response: {"result": 3.3333333333333335, ...}
```

### Chained Operations (Manual)
```bash
# Step 1: Add 10 + 5 = 15
# Step 2: Multiply 15 × 2 = 30
# Step 3: Divide 30 ÷ 3 = 10
# Step 4: Subtract 10 - 5 = 5
```

---

## Testing All Operations

```bash
# Test script (tests first 3 operations)
./test-all.sh

# Manual test of divide
curl -X POST http://localhost:8080/api/v1/calculate/divide \
  -H "Content-Type: application/json" \
  -d '{"operand1": 10, "operand2": 5}'
```

---

## Plugin Registration

Plugins are registered in `PluginRegistry.java`:

```java
registry.put("add_numbers", new PluginSpec(
    "add_numbers", "1.0.0", "java",
    "java-runtime-supervisor:9091", "AddPlugin"
));

registry.put("multiply_numbers", new PluginSpec(
    "multiply_numbers", "1.0.0", "java",
    "java-runtime-supervisor:9091", "MultiplyPlugin"
));

registry.put("subtract_numbers", new PluginSpec(
    "subtract_numbers", "1.0.0", "python",
    "python-runtime-supervisor:9092", "subtract_plugin.py"
));

registry.put("divide_numbers", new PluginSpec(
    "divide_numbers", "1.0.0", "python",
    "python-runtime-supervisor:9092", "divide_plugin.py"
));
```

Total registered plugins: **4**

---

## Worker Container Lifecycle

For each request:

1. **Allocation** (~5s):
   - Gateway → Runtime Supervisor (gRPC AllocateWorker)
   - Supervisor spawns Docker container
   - DNS propagation wait

2. **Execution** (~0.1s):
   - Gateway → Worker (gRPC PPP Init)
   - Gateway → Worker (gRPC PPP Invoke)
   - Worker streams progress and result

3. **Cleanup** (~1s):
   - Gateway closes gRPC channel
   - Gateway → Runtime Supervisor (gRPC ReleaseWorker)
   - Supervisor stops container (auto-removed)

**Total**: ~6 seconds (ephemeral mode)

---

## Error Codes

| Status | Scenario | Example |
|--------|----------|---------|
| **200 OK** | Successful calculation | Normal operations |
| **500 Internal Server Error** | Division by zero | operand2 = 0 |
| **500 Internal Server Error** | Worker allocation failed | Out of resources |
| **500 Internal Server Error** | Plugin execution error | Invalid input |
| **500 Internal Server Error** | Worker crash | Container failure |

All errors return consistent JSON format with timestamp, path, status, and error message.

---

## Rate Limits & Quotas

**Current**: None (POC)  
**Production**: Should implement hierarchical rate limiting per architecture notes.

---

## Monitoring & Observability

**Logs Available**:
```bash
# Gateway logs (requests, routing)
docker-compose logs -f plugin-gateway

# Java Runtime Supervisor (worker lifecycle)
docker-compose logs -f java-runtime-supervisor

# Python Runtime Supervisor (worker lifecycle)
docker-compose logs -f python-runtime-supervisor

# Worker logs (during execution)
docker logs worker-10001  # Java worker
docker logs worker-20001  # Python worker
```

---

## Next Steps

1. **Add More Plugins**: Follow the pattern in `plugins/java-plugins/` or `plugins/python-plugins/`
2. **Implement Worker Pooling**: Reduce latency from 6s to ~100ms
3. **Add Authentication**: Implement mTLS and token validation
4. **Rate Limiting**: Add hierarchical rate limits
5. **Metrics**: Add Prometheus metrics collection

---

**Last Updated**: October 20, 2025  
**Version**: 1.0.0-SNAPSHOT

