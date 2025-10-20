# Quick Start Guide

## Prerequisites

Ensure you have the following installed:
- Java 17 or higher
- Docker and Docker Compose
- Maven 3.8+ (or use included wrapper)

## Step-by-Step Setup

### 1. Build Plugin Worker Images

This is the FIRST step - build the plugin worker images that will be dynamically spawned:

```bash
cd /Users/ramjana/dev/AI/agentic-server-platform-poc
chmod +x build-images.sh
./build-images.sh
```

This will:
- Compile all Maven projects
- Build 3 plugin worker Docker images:
  - `java-plugin-add:latest`
  - `java-plugin-multiply:latest`
  - `python-plugin-subtract:latest`

⏱️ **Expected time**: 5-10 minutes (first build)

### 2. Start the Platform

Launch the Plugin Gateway and Runtime Supervisors:

```bash
docker-compose up -d
```

This starts:
- **plugin-gateway** (port 8080) - Entry point
- **java-runtime-supervisor** (port 9091) - Java worker manager
- **python-runtime-supervisor** (port 9092) - Python worker manager

### 3. Verify Services are Running

```bash
docker-compose ps
```

You should see 3 containers running:
```
NAME                        STATUS
plugin-gateway              Up
java-runtime-supervisor     Up  
python-runtime-supervisor   Up
```

Check logs:
```bash
docker-compose logs -f
```

Wait for log messages indicating services are ready (usually 10-20 seconds).

### 4. Test the Platform

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

### 5. Run All Tests

Use the provided test script:

```bash
./test-all.sh
```

### 6. Observe Worker Containers

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

### 7. View Logs

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

### 8. Stop the Platform

```bash
docker-compose down
```

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
./build-images.sh
```

### Issue: Maven build fails
```bash
# Clean and rebuild
./mvnw clean install -DskipTests

# Or with Docker
docker run --rm -v $(pwd):/app -w /app maven:3.9-eclipse-temurin-17 mvn clean install -DskipTests
```

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

