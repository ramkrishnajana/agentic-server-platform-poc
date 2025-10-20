# Quick Start Guide

## Prerequisites

Ensure you have the following installed:
- **Java 17 or higher** (tested with Java 25)
- **Docker and Docker Compose**
- **Maven 3.8+** (or use included wrapper)

⚠️ **Note**: This project does NOT use Lombok due to Java 25 compatibility issues. All code uses manual implementations.

## Step-by-Step Setup

### 1. Build Maven Projects

First, compile all Java modules:

```bash
cd /Users/ramjana/dev/AI/agentic-server-platform-poc
./mvnw package -DskipTests
```

⏱️ **Expected time**: 1-2 minutes (first build)

**Troubleshooting**: If you see protobuf cleanup errors with `clean`, it's safe to ignore - just use `package` without `clean`.

### 2. Build Plugin Worker Images

Build the 3 plugin worker Docker images:

```bash
# Build Java plugin workers
docker build -t java-plugin-add:latest -f java-plugin-add/Dockerfile .
docker build -t java-plugin-multiply:latest -f java-plugin-multiply/Dockerfile .

# Build Python plugin worker  
docker build -t python-plugin-subtract:latest -f python-plugin-subtract/Dockerfile .
```

⏱️ **Expected time**: 2-3 minutes (downloads base images on first run)

**Note**: Base images changed from Alpine to Debian for ARM64/M1 Mac compatibility.

### 3. Start the Platform

Launch the Plugin Gateway and Runtime Supervisors:

```bash
docker-compose up -d
```

This builds and starts:
- **plugin-gateway** (port 8080) - Entry point
- **java-runtime-supervisor** (port 9091) - Java worker manager
- **python-runtime-supervisor** (port 9092) - Python worker manager

⏱️ **Wait 10-15 seconds** for all services to initialize.

### 4. Verify Services are Running

```bash
docker-compose ps
```

You should see 3 containers running with "Up" status:
```
NAME                        STATUS
plugin-gateway              Up
java-runtime-supervisor     Up  
python-runtime-supervisor   Up
```

Check logs to confirm all services started successfully:
```bash
docker-compose logs plugin-gateway | grep "Started"
```

You should see: `Started PluginGatewayApplication in X seconds`

### 5. Test the Platform

#### Test Add Operation (Java Plugin)

```bash
curl -X POST http://localhost:8080/api/v1/calculate/add \
  -H "Content-Type: application/json" \
  -d '{"operand1": 10, "operand2": 5}'
```

**Expected Output:**
```json
{"result":15.0,"operation":"add","operand1":10.0,"operand2":5.0}
```

#### Test Multiply Operation (Java Plugin)

```bash
curl -X POST http://localhost:8080/api/v1/calculate/multiply \
  -H "Content-Type: application/json" \
  -d '{"operand1": 10, "operand2": 5}'
```

**Expected Output:**
```json
{"result":50.0,"operation":"multiply","operand1":10.0,"operand2":5.0}
```

#### Test Subtract Operation (Python Plugin)

```bash
curl -X POST http://localhost:8080/api/v1/calculate/subtract \
  -H "Content-Type: application/json" \
  -d '{"operand1": 10, "operand2": 5}'
```

**Expected Output:**
```json
{"result":5.0,"operation":"subtract","operand1":10.0,"operand2":5.0}
```

### 6. Run All Tests

Use the provided test script:

```bash
chmod +x test-all.sh
./test-all.sh
```

**Expected Output**:
```
Testing Agentic Server Platform POC
====================================
1. Testing Add Operation (Java Plugin)
Response: {"result":15.0,"operation":"add","operand1":10.0,"operand2":5.0}

2. Testing Multiply Operation (Java Plugin)
Response: {"result":50.0,"operation":"multiply","operand1":10.0,"operand2":5.0}

3. Testing Subtract Operation (Python Plugin)
Response: {"result":5.0,"operation":"subtract","operand1":10.0,"operand2":5.0}

All tests completed!
```

### 7. Observe Worker Containers

While a plugin is executing, you can see the worker container:

**Terminal 1**: Run a calculation
```bash
curl -X POST http://localhost:8080/api/v1/calculate/add \
  -H "Content-Type: application/json" \
  -d '{"operand1": 100, "operand2": 200}'
```

**Terminal 2**: Quickly check containers (within 2-3 seconds)
```bash
docker ps | grep worker
```

You should see a container like `worker-10001`.

After the request completes, the worker container will be automatically destroyed.

**Note**: Worker containers use the network name for communication, not port mapping, so they won't appear on host ports.

### 8. View Logs

**Plugin Gateway logs:**
```bash
docker-compose logs -f plugin-gateway
```

**Java Runtime Supervisor logs:**
```bash
docker-compose logs -f java-runtime-supervisor
```

**Python Runtime Supervisor logs:**
```bash
docker-compose logs -f python-runtime-supervisor
```

### 9. Stop the Platform

```bash
docker-compose down
```

This will stop and remove all platform service containers. Worker containers are automatically removed after use.

## What's Happening Behind the Scenes?

When you make a request:

1. **Plugin Gateway** receives REST request
2. **Gateway** looks up plugin in registry (language, runtime address)
3. **Gateway** calls **Runtime Supervisor** via gRPC to allocate worker
4. **Runtime Supervisor** spawns a Docker container for the plugin
5. **Gateway** communicates with **Plugin Worker** via gRPC
6. **Worker** streams progress and result back
7. **Gateway** tells **Runtime Supervisor** to release worker
8. **Runtime Supervisor** stops and removes container
9. **Gateway** returns result to client

## Troubleshooting

### Issue: Port 8080 already in use
```bash
# Find process using port
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or change port in docker-compose.yml
```

### Issue: Docker socket permission denied
```bash
# Add user to docker group
sudo usermod -aG docker $USER

# Log out and back in
```

### Issue: Worker containers not spawning
```bash
# Check if worker images exist
docker images | grep plugin

# Rebuild if missing
docker build -t java-plugin-add:latest -f java-plugin-add/Dockerfile .
docker build -t java-plugin-multiply:latest -f java-plugin-multiply/Dockerfile .
docker build -t python-plugin-subtract:latest -f python-plugin-subtract/Dockerfile .
```

### Issue: Plugin Gateway won't start (gRPC errors)
Check that gRPC version 1.58.0 is being used (not 1.60.0). The pom.xml should have:
```xml
<grpc.version>1.58.0</grpc.version>
<protobuf.version>3.24.0</protobuf.version>
```

### Issue: Maven build fails
```bash
# If clean fails due to protobuf plugin, build without clean
./mvnw package -DskipTests

# Or manually clean and rebuild
rm -rf proto/target common/target plugin-gateway/target
./mvnw package -DskipTests
```

### Issue: Lombok compilation errors with Java 25
**This project no longer uses Lombok**. If you see old Lombok-related errors, the code has been updated with manual implementations. Just rebuild.

## Understanding the Logs

### Plugin Gateway Log Example
```
INFO  - Executing add_numbers operation
INFO  - Worker allocated: worker-10001
INFO  - Plugin execution completed: 15.0
```

### Runtime Supervisor Log Example
```
INFO  - AllocateWorker called for: add_numbers
INFO  - Starting worker worker-10001
INFO  - Worker worker-10001 started on port 10001
INFO  - Releasing worker worker-10001
```

### Plugin Worker Log Example
```
INFO  - Plugin initialized for tenant: demo-tenant
INFO  - Adding 10.0 + 5.0
INFO  - Addition completed: 15.0
```

## Next Steps

- Read the [README.md](README.md) for architecture details
- Review [ARCHITECTURE.md](ARCHITECTURE.md) for in-depth documentation
- Explore the source code in each module
- Try modifying plugin logic
- Add your own plugin
- Implement worker pooling
- Add authentication

## Creating Your Own Plugin

### 1. Create a new Maven module

**For Java Plugin:**
```bash
cd agentic-server-platform-poc
mkdir my-plugin
# Copy structure from java-plugin-add
# Modify the plugin logic
```

### 2. Register in Plugin Registry

Edit `plugin-gateway/src/main/java/.../PluginRegistry.java`:

```java
registry.put("my_operation", new PluginSpec(
    "my_operation",
    "1.0.0",
    "java",
    "java-runtime-supervisor:9091",
    "MyPlugin"
));
```

### 3. Add controller endpoint

Edit `plugin-gateway/src/main/java/.../CalculationController.java`:

```java
@PostMapping("/my-operation")
public ResponseEntity<CalculationResult> myOperation(@RequestBody CalculationRequest request) {
    // ...
}
```

### 4. Build and test

```bash
./mvnw clean package -DskipTests
docker build -t my-plugin:latest -f my-plugin/Dockerfile .
docker-compose restart
```

## Architecture at a Glance

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ REST
       ▼
┌──────────────────┐
│ Plugin Gateway   │◄──── Plugin Registry
│   (Port 8080)    │
└────────┬─────────┘
         │ gRPC (Runtime Supervisor API)
         ├────────────────┬────────────────┐
         ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────┐
│ Java Runtime │  │Python Runtime│  │  Future  │
│  Supervisor  │  │  Supervisor  │  │ Runtimes │
│  (Port 9091) │  │  (Port 9092) │  │          │
└──────┬───────┘  └──────┬───────┘  └──────────┘
       │                  │
       │ Spawns           │ Spawns
       ▼                  ▼
┌──────────────┐  ┌──────────────┐
│Java Workers  │  │Python Workers│
│(Containers)  │  │(Containers)  │
│ - Add        │  │ - Subtract   │
│ - Multiply   │  │              │
└──────────────┘  └──────────────┘
```

## Performance Tips

1. **First request is slow** (~2-3 seconds) due to container spawn
2. **Subsequent requests** to different plugins are also slow (ephemeral mode)
3. **Production**: Implement worker pooling for 10-100x improvement
4. **Concurrent requests**: Platform supports parallel execution

## Security Note

⚠️ **This is a POC without security**:
- No authentication
- No rate limiting
- No input sanitization
- Docker socket exposed

**Do not use in production without**:
- Adding mTLS
- Implementing authentication
- Adding rate limiting
- Hardening containers
- Network policies

