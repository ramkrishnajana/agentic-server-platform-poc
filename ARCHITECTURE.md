# Agentic Server Platform POC - Architecture Documentation

## Overview

This POC demonstrates a production-grade plugin runtime architecture for an Agentic Server Platform, following the specifications from the architecture notes. The system enables dynamic execution of isolated plugins in separate containers with multi-language support.

**Key Implementation Notes**:
- ✅ **Java 25 Compatible**: No Lombok dependency - uses manual implementations
- ✅ **ARM64 Support**: Works on M1/M2/M3 Macs with non-Alpine base images
- ✅ **gRPC 1.58.0**: Compatible with Spring Boot gRPC starters
- ✅ **Container Networking**: Workers communicate via Docker network names (no port mapping conflicts)

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
- Custom Docker network (`agentic-server-platform-poc_agentic-network`)
- Workers communicate via container names (no host port mapping needed)
- Runtime supervisors have Docker socket access (`/var/run/docker.sock`)
- Plugin Gateway communicates with workers via gRPC using container names

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

### Worker Communication
- Workers expose gRPC service on port **8080** internally
- Communication via Docker network using **container names** (e.g., `worker-10001:8080`)
- No host port mapping for workers (prevents port conflicts)
- Runtime supervisors expose port ranges 10000-10100 (Java) and 20000-20100 (Python) for potential future use

## Build Process

1. **Compile Protobuf**: Maven protobuf plugin generates Java and Python code from `.proto` files
2. **Build Maven Projects**: All Java modules compiled with Java 17 target (annotation processing disabled)
3. **Create Worker Docker Images**:
   - Use simple Dockerfiles that copy pre-built JARs (no multi-stage builds)
   - Base image: `eclipse-temurin:17-jre` (Debian-based for ARM64 compatibility)
   - Worker images: java-plugin-add, java-plugin-multiply, python-plugin-subtract
4. **Docker Compose Up**: Builds platform service images and starts all services
   - Builds: plugin-gateway, java-runtime-supervisor, python-runtime-supervisor
   - Installs Docker CLI inside runtime supervisors for worker management
   - Creates custom network: `agentic-server-platform-poc_agentic-network`

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

1. **No Worker Pooling**: Every request spawns fresh container (~2-3 second cold start)
2. **No Native Compilation**: Java applications run on JVM (not GraalVM native)
3. **No Authentication**: All endpoints open (POC only)
4. **No Rate Limiting**: No protection against abuse
5. **No Persistence**: No session state persistence
6. **Basic Error Handling**: Limited error recovery
7. **No Metrics**: No Prometheus/Grafana integration
8. **Docker-in-Docker**: Runtime supervisors need Docker socket access
9. **No Lombok**: Manually implemented getters/setters for Java 25 compatibility
10. **Build Workaround**: Maven `clean` may fail with protobuf plugin - use `package` instead

## Future Enhancements

1. **Worker Pooling**: Implement warm container pools for frequently-used plugins
2. **GraalVM Native**: Add native image compilation for faster startup
3. **Authentication**: Implement mTLS and token-based auth
4. **Rate Limiting**: Add hierarchical rate limiting (tenant/user/primitive)
5. **Session Persistence**: Implement checkpoint/resume with Redis
6. **Monitoring**: Add Prometheus metrics and Grafana dashboards
7. **Kubernetes**: Create K8s manifests for production deployment
8. **Resilience**: Add circuit breakers and retry logic
9. **Health Checks**: Comprehensive health monitoring and auto-recovery
10. **Multi-tenancy**: Resource quotas and isolation per tenant
11. **Alpine Images**: Once ARM64 Alpine support improves, switch back for smaller images
12. **Build Optimization**: Fix protobuf plugin cleanup issues

## Performance Characteristics

### Latency
- Container spawn time: ~2-3 seconds (ephemeral mode)
- gRPC communication: <10ms
- Plugin execution: ~50-100ms
- Total request time: ~2-4 seconds (cold start with container spawn)

### Throughput
- Limited by container spawn rate (~1-2 workers/second per runtime supervisor)
- Parallel execution supported (multiple workers can run concurrently)
- Worker pools would improve throughput by 10x-100x

### Resource Usage
- Plugin Gateway: ~330MB image, ~200MB RAM
- Java Runtime Supervisor: ~670MB image, ~150MB RAM
- Python Runtime Supervisor: ~686MB image, ~150MB RAM
- Java Worker containers: ~320MB image, ~100MB RAM each
- Python Worker containers: ~221MB image, ~50MB RAM each

**Note**: ARM64/M1 Mac uses larger Debian-based images instead of Alpine for compatibility.

## Technical Decisions & Fixes

### Java 25 Compatibility
**Issue**: Lombok 1.18.34 incompatible with Java 25 (missing `TypeTag.UNKNOWN` field)  
**Solution**: Removed all Lombok dependencies and implemented manual:
- Logger declarations using `LoggerFactory.getLogger()`
- Getters and setters for all model classes
- Constructors for dependency injection

**Files Modified**:
- `CalculationRequest.java` - Manual getters/setters
- `CalculationResult.java` - Manual getters/setters  
- `PluginRegistry.PluginSpec` - Manual constructor and getters
- `WorkerManager.WorkerProcess` - Manual constructor and getters
- All service classes - Manual logger and constructor

**Configuration Changes**:
- Disabled annotation processing in all modules (`<proc>none</proc>`)
- Removed `lombok.version` property from parent POM
- Removed Lombok dependencies from all child POMs

### ARM64/M1 Mac Support
**Issue**: `eclipse-temurin:17-jdk-alpine` not available for ARM64 architecture  
**Solution**: Changed all Dockerfiles to use `eclipse-temurin:17-jre` (Debian-based)

**Trade-off**: Larger image sizes (~200-400MB more) but better compatibility

### gRPC Version Compatibility
**Issue**: grpc-spring-boot-starter 2.15.0 incompatible with gRPC 1.60.0  
**Solution**: Downgraded to gRPC 1.58.0 and Protobuf 3.24.0

**POM Changes**:
```xml
<grpc.version>1.58.0</grpc.version>
<protobuf.version>3.24.0</protobuf.version>
```

### Docker Networking
**Issue**: Port mapping conflicts when runtime supervisors spawn workers  
**Solution**: 
- Workers use container names for communication (e.g., `worker-10001:8080`)
- Removed `-p` flag from worker spawn commands
- Fixed network name: `agentic-server-platform-poc_agentic-network` (docker-compose adds prefix)

**Code Changes**:
- `WorkerManager.java`: Removed port mapping from docker run command
- `runtime_supervisor.py`: Removed port mapping from docker run command
- `PluginExecutionService.getWorkerAddress()`: Returns `workerId + ":8080"` instead of host:port

### Build Simplification
**Issue**: Multi-stage Docker builds with Maven wrapper files not included in context  
**Solution**: Simplified Dockerfiles to copy pre-built JARs from `target/` directories

**Before**:
```dockerfile
FROM eclipse-temurin:17-jdk AS builder
COPY . .
RUN ./mvnw clean package
FROM eclipse-temurin:17-jre
COPY --from=builder /workspace/target/*.jar app.jar
```

**After**:
```dockerfile
FROM eclipse-temurin:17-jre
COPY plugin-name/target/*.jar app.jar
```

**Benefits**:
- Faster Docker builds (no Maven compilation in Docker)
- Simpler debugging
- Smaller build context

### Maven Protobuf Plugin
**Issue**: Cleanup phase fails intermittently when deleting protobuf dependencies  
**Workaround**: Use `./mvnw package` instead of `./mvnw clean package`

**Root Cause**: File system race condition or permission issue with temp proto files

