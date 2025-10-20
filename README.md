# Agentic Server Platform POC

This is a Proof of Concept implementation of the Agentic Server Platform demonstrating:
- **Plugin Gateway**: Entry point for all plugin execution requests
- **Runtime Supervisors**: Manage plugin worker lifecycle (Java and Python)
- **Isolated Plugin Workers**: Each plugin runs in a separate container

## Architecture

```
┌─────────────────────┐
│  Plugin Gateway     │  (Spring Boot on port 8080)
│  (REST API)         │
└──────────┬──────────┘
           │
           ├─────────────────────┬─────────────────────┐
           │                     │                     │
    ┌──────▼──────────┐   ┌─────▼──────────┐  ┌──────▼──────────┐
    │ Java Runtime    │   │ Python Runtime │  │ (Future         │
    │ Supervisor      │   │ Supervisor     │  │  Runtimes)      │
    │ (gRPC :9091)    │   │ (gRPC :9092)   │  │                 │
    └──────┬──────────┘   └─────┬──────────┘  └─────────────────┘
           │                     │
    ┌──────▼──────────┐   ┌─────▼──────────┐
    │ Java Workers    │   │ Python Workers │
    │ (Containers)    │   │ (Containers)   │
    │ - Add Plugin    │   │ - Subtract     │
    │ - Multiply      │   │   Plugin       │
    │   Plugin        │   │                │
    └─────────────────┘   └────────────────┘
```

## Technology Stack

- **Spring Boot 3.2**: Plugin Gateway and Java Runtime Supervisor
- **gRPC 1.58.0**: Inter-service communication (Runtime Supervisor API and Platform-Plugin Protocol)
- **Protocol Buffers 3.24.0**: Message definitions
- **Docker**: Containerization and isolation (eclipse-temurin:17-jre base images)
- **Maven 3.9**: Build tool for Java components
- **Python 3.11**: Python Runtime Supervisor and Python plugins
- **Java 17-25**: Compiled with Java 17 target, tested with Java 25

**Note**: No Lombok - all code uses manual implementations for Java 25 compatibility.

## Plugins

1. **Add Numbers** (Java) - Adds two numbers
2. **Multiply Numbers** (Java) - Multiplies two numbers
3. **Subtract Numbers** (Python) - Subtracts two numbers

## Prerequisites

- **Java 17 or higher** (tested with Java 25)
- **Maven 3.8+** (or use the included Maven wrapper)
- **Docker and Docker Compose**
- **Python 3.11+** (for Python components)

⚠️ **Important**: This project does NOT use Lombok to ensure compatibility with Java 17-25. All getters, setters, and loggers are manually implemented.

## Build and Run

### 1. Build Maven Projects

First, compile all Java modules:

```bash
./mvnw clean package -DskipTests
```

**Note**: The `clean` goal may fail due to protobuf plugin cleanup issues. If this happens, use:
```bash
./mvnw package -DskipTests
```

### 2. Build Plugin Worker Images

Build the plugin worker Docker images:

```bash
# Build Java plugin workers
docker build -t java-plugin-add:latest -f java-plugin-add/Dockerfile .
docker build -t java-plugin-multiply:latest -f java-plugin-multiply/Dockerfile .

# Build Python plugin worker
docker build -t python-plugin-subtract:latest -f python-plugin-subtract/Dockerfile .
```

### 3. Start the Platform

Start the Plugin Gateway and Runtime Supervisors:

```bash
docker-compose up -d
```

This will:
- Build the platform service images (plugin-gateway, java-runtime-supervisor, python-runtime-supervisor)
- Start all services:
  - Plugin Gateway (http://localhost:8080)
  - Java Runtime Supervisor (gRPC port 9091)
  - Python Runtime Supervisor (gRPC port 9092)

Wait 10-15 seconds for services to fully initialize.

### 4. Verify Services

```bash
# Check all services are running
docker-compose ps

# You should see 3 services:
# - plugin-gateway (Up)
# - java-runtime-supervisor (Up)
# - python-runtime-supervisor (Up)

# View logs
docker-compose logs -f
```

## Testing the Platform

### Test Add Operation (Java Plugin)

```bash
curl -X POST http://localhost:8080/api/v1/calculate/add \
  -H "Content-Type: application/json" \
  -d '{
    "operand1": 10,
    "operand2": 5
  }'
```

Expected response:
```json
{
  "result": 15.0,
  "operation": "add",
  "operand1": 10.0,
  "operand2": 5.0
}
```

### Test Multiply Operation (Java Plugin)

```bash
curl -X POST http://localhost:8080/api/v1/calculate/multiply \
  -H "Content-Type: application/json" \
  -d '{
    "operand1": 10,
    "operand2": 5
  }'
```

Expected response:
```json
{
  "result": 50.0,
  "operation": "multiply",
  "operand1": 10.0,
  "operand2": 5.0
}
```

### Test Subtract Operation (Python Plugin)

```bash
curl -X POST http://localhost:8080/api/v1/calculate/subtract \
  -H "Content-Type: application/json" \
  -d '{
    "operand1": 10,
    "operand2": 5
  }'
```

Expected response:
```json
{
  "result": 5.0,
  "operation": "subtract",
  "operand1": 10.0,
  "operand2": 5.0
}
```

## How It Works

1. **Request Flow**:
   - Client sends HTTP POST to Plugin Gateway
   - Gateway looks up plugin spec from Plugin Registry
   - Gateway sends gRPC request to appropriate Runtime Supervisor
   - Runtime Supervisor spawns a new worker container
   - Gateway communicates with worker via Platform-Plugin Protocol (PPP)
   - Worker executes the operation and streams back progress/results
   - Gateway releases the worker (container is destroyed)

2. **Isolation**:
   - Each plugin execution runs in a fresh container
   - Containers are destroyed after use (--rm flag)
   - No shared state between plugin executions
   - Resource limits can be enforced per container

3. **Communication Protocols**:
   - **REST**: Client → Plugin Gateway
   - **Runtime Supervisor API (gRPC)**: Plugin Gateway → Runtime Supervisors
   - **Platform-Plugin Protocol (gRPC)**: Gateway → Plugin Workers

## Viewing Worker Containers

While a plugin is executing, you can see the worker container:

```bash
# In another terminal, run a calculation
curl -X POST http://localhost:8080/api/v1/calculate/add \
  -H "Content-Type: application/json" \
  -d '{"operand1": 100, "operand2": 200}'

# Quickly check running containers
docker ps | grep worker
```

You should see containers like `worker-10001`, `worker-10002`, etc.

## Cleanup

Stop and remove all containers:

```bash
docker-compose down
```

Remove plugin worker images:

```bash
docker rmi java-plugin-add:latest
docker rmi java-plugin-multiply:latest
docker rmi python-plugin-subtract:latest
```

## Project Structure

```
agentic-server-platform-poc/
├── proto/                          # Protocol Buffer definitions
│   └── src/main/proto/
│       ├── runtime_supervisor.proto  # Runtime Supervisor API
│       └── plugin_protocol.proto     # Platform-Plugin Protocol (PPP)
├── common/                         # Shared models
├── plugin-gateway/                 # Spring Boot Gateway
│   ├── src/main/java/
│   │   └── com/webex/agentic/gateway/
│   │       ├── controller/         # REST controllers
│   │       └── service/            # Business logic
│   └── Dockerfile
├── java-runtime-supervisor/        # Java worker manager
│   ├── src/main/java/
│   │   └── com/webex/agentic/runtime/java/
│   └── Dockerfile
├── python-runtime-supervisor/      # Python worker manager
│   ├── runtime_supervisor.py
│   └── Dockerfile
├── java-plugin-add/               # Add plugin
│   └── Dockerfile
├── java-plugin-multiply/          # Multiply plugin
│   └── Dockerfile
├── python-plugin-subtract/        # Subtract plugin
│   └── Dockerfile
├── docker-compose.yml             # Platform orchestration
└── build-images.sh                # Build script
```

## Key Features Demonstrated

✅ **Plugin Architecture**: Extensible plugin system with registry
✅ **Multi-Language Support**: Java and Python plugins
✅ **Container Isolation**: Each plugin runs in isolated container
✅ **Dynamic Worker Management**: Containers spawned on-demand and destroyed after use
✅ **gRPC Communication**: High-performance inter-service communication
✅ **Protocol Abstraction**: Separate Runtime Supervisor API and Plugin Protocol
✅ **Progress Streaming**: Plugins can stream progress updates
✅ **Lifecycle Management**: Init, Invoke, Health endpoints

## Architecture Alignment

This POC demonstrates the following concepts from the architecture document:

1. **Plugin Runtime Services**:
   - ✅ Plugin Manager (Worker lifecycle in Runtime Supervisors)
   - ✅ Plugin Gateway (Routes requests to appropriate runtime)
   - ✅ Language Runtime Services (Java and Python supervisors)

2. **Isolation Model**:
   - ✅ One OS process per plugin (separate containers)
   - ✅ Hard boundary between plugins
   - ✅ Independent crash domains

3. **Protocols**:
   - ✅ Runtime Supervisor API (Gateway ↔ Supervisor)
   - ✅ Platform-Plugin Protocol (PPP - Gateway ↔ Workers)

4. **Worker Lifecycle**:
   - ✅ Ephemeral workers (spawn per request)
   - ✅ Dynamic container management
   - ✅ Clean shutdown and resource cleanup

## Next Steps for Production

1. **Resource Limits**: Add CPU/memory limits to worker containers
2. **Worker Pooling**: Implement warm worker pools for high-frequency plugins
3. **Monitoring**: Add metrics collection (Prometheus/Grafana)
4. **Security**: Implement mTLS between components
5. **Rate Limiting**: Add hierarchical rate limiting
6. **Session Persistence**: Implement checkpoint/resume for long-running tasks
7. **Multi-tenant Support**: Add tenant isolation and quotas
8. **Health Checks**: Implement comprehensive health monitoring
9. **Circuit Breakers**: Add resilience patterns
10. **Kubernetes Deployment**: Create K8s manifests for production deployment

## Troubleshooting

### Port conflicts
If ports 8080, 9091, or 9092 are in use:
```bash
# Find process using port
lsof -i :8080
# Kill the process
kill -9 <PID>
```

### Docker socket permission denied
```bash
# Add user to docker group
sudo usermod -aG docker $USER
# Log out and back in
```

### Proto compilation errors
```bash
# If clean fails due to protobuf plugin cleanup, build without clean
./mvnw package -DskipTests

# Or manually clean target directories first
rm -rf proto/target common/target plugin-gateway/target
./mvnw package -DskipTests
```

### Services fail to start
```bash
# Check logs for specific service
docker-compose logs plugin-gateway
docker-compose logs java-runtime-supervisor
docker-compose logs python-runtime-supervisor

# Rebuild specific service
docker-compose up -d --build plugin-gateway
```

## License

This is a proof-of-concept for demonstration purposes.

