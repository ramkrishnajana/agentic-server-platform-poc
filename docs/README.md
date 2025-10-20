# Architecture Diagrams

This directory contains detailed UML sequence diagrams for the Agentic Server Platform POC.

## Available Diagrams

### Sequence Diagrams

1. **`add-plugin-sequence.puml`** - PlantUML diagram for Add operation (Java worker)
2. **`subtract-plugin-sequence.puml`** - PlantUML diagram for Subtract operation (Python worker)
3. **`API-FLOW-DIAGRAMS.md`** - Mermaid diagrams (viewable in GitHub)

## Viewing the Diagrams

### Option 1: GitHub (Mermaid diagrams)
Simply open `API-FLOW-DIAGRAMS.md` on GitHub - Mermaid diagrams render automatically.

### Option 2: PlantUML (for *.puml files)

**Online**:
- Visit http://www.plantuml.com/plantuml/uml/
- Paste the content from `.puml` files

**VSCode**:
```bash
# Install PlantUML extension
code --install-extension jebbs.plantuml
# Open .puml file and press Alt+D to preview
```

**Command Line**:
```bash
# Install PlantUML
brew install plantuml  # macOS
# or
apt-get install plantuml  # Linux

# Generate PNG
plantuml add-plugin-sequence.puml
plantuml subtract-plugin-sequence.puml
```

### Option 3: IntelliJ IDEA
PlantUML plugin available - diagrams render inline in IDE.

## Diagram Coverage

### Complete Request Flow
Both diagrams show the complete end-to-end flow:

1. ✅ Client HTTP POST request
2. ✅ Plugin Registry lookup
3. ✅ PluginRef and Context building
4. ✅ Runtime Supervisor gRPC call (AllocateWorker)
5. ✅ Docker container spawn
6. ✅ DNS propagation wait
7. ✅ gRPC PPP tunnel establishment (Init)
8. ✅ Plugin execution with streaming (Invoke → Progress → Completed)
9. ✅ Result parsing and validation
10. ✅ gRPC channel cleanup
11. ✅ Worker release (ReleaseWorker)
12. ✅ Container destruction
13. ✅ HTTP response to client

### Technology Highlights

**Reactive Stack (WebFlux)**:
- Controllers return `Mono<CalculationResult>`
- Netty web server (non-blocking)
- Reactor patterns: `Mono.fromCallable()`, `subscribeOn()`

**gRPC Communication**:
- Runtime Supervisor API (Gateway ↔ Supervisors)
- Platform-Plugin Protocol / PPP (Gateway ↔ Workers)
- Streaming support for progress updates

**Container Lifecycle**:
- Ephemeral workers (spawn per request)
- Docker-in-Docker pattern
- Auto-removal (--rm flag)
- Network-based communication

## Key Differences: Java vs Python Workers

| Aspect | Add (Java) | Subtract (Python) |
|--------|------------|-------------------|
| Supervisor Port | 9091 | 9092 |
| Worker ID | worker-100XX | worker-200XX |
| Container Image | java-plugin-add:latest | python-plugin-subtract:latest |
| Startup Time | ~3-4s | ~2-3s |
| Memory | ~100MB | ~50MB |
| gRPC Implementation | Java gRPC (Netty) | Python gRPC |

## Timing Breakdown

**End-to-End Request Time**: ~5-7 seconds (ephemeral mode)

- **Allocation Phase** (4-5s):
  - Docker container spawn: ~4s
  - DNS propagation wait: ~1s

- **Execution Phase** (0.1-0.2s):
  - gRPC Init: ~20ms
  - Plugin invoke: ~80ms
  - Progress streaming: included

- **Cleanup Phase** (~1s):
  - gRPC channel close: ~100ms
  - Container stop: ~500ms
  - Auto-remove: ~100ms

## Future Optimizations

With **Worker Pooling** + **GraalVM Native**:
- Total time: **~100ms** (50-80x improvement)
- Worker startup: 0ms (already running)
- Native startup: ~50-100ms (vs 2-3s JVM)
- Execution: ~60ms
- Cleanup: None (worker returned to pool)

## Notes

- All timings are approximate and measured on ARM64/M1 Mac
- Production performance may vary based on hardware and configuration
- Native image builds take longer (~5-10 min) but run much faster
- WebFlux provides non-blocking I/O during long worker spawn times


