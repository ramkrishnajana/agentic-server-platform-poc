package com.webex.agentic.gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.webex.agentic.common.model.CalculationRequest;
import com.webex.agentic.common.model.CalculationResult;
import com.webex.agentic.proto.ppp.*;
import com.webex.agentic.proto.supervisor.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Reactive service that executes plugin operations using WebFlux
 */
@Service
public class PluginExecutionService {
    
    private static final Logger log = LoggerFactory.getLogger(PluginExecutionService.class);

    private final PluginRegistry pluginRegistry;
    private final RuntimeSupervisorClient runtimeClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public PluginExecutionService(PluginRegistry pluginRegistry, RuntimeSupervisorClient runtimeClient) {
        this.pluginRegistry = pluginRegistry;
        this.runtimeClient = runtimeClient;
    }

    public Mono<CalculationResult> executeCalculation(String operation, CalculationRequest request) {
        return Mono.fromCallable(() -> executeCalculationBlocking(operation, request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private CalculationResult executeCalculationBlocking(String operation, CalculationRequest request) throws Exception {
        log.info("Executing {} operation: {} on {}", operation, request.getOperand1(), request.getOperand2());

        // Get plugin spec
        PluginRegistry.PluginSpec pluginSpec = pluginRegistry.getPlugin(operation);
        
        // Build plugin ref
        PluginRef pluginRef = PluginRef.newBuilder()
            .setId(pluginSpec.getId())
            .setVersion(pluginSpec.getVersion())
            .setLanguage(pluginSpec.getLanguage())
            .setEntrypoint(pluginSpec.getEntrypoint())
            .build();

        // Build context
        String requestId = UUID.randomUUID().toString();
        com.webex.agentic.proto.supervisor.Context supervisorContext = 
            com.webex.agentic.proto.supervisor.Context.newBuilder()
                .setTenantId("demo-tenant")
                .setUserId("demo-user")
                .setSessionId(UUID.randomUUID().toString())
                .setCorrelationId(requestId)
                .setRequestId(requestId)
                .setPrimitive(operation)
                .build();

        // Allocate worker
        AllocateWorkerResponse allocResponse = runtimeClient.allocateWorker(
            pluginSpec.getRuntimeAddress(),
            pluginRef,
            supervisorContext
        );

        if (allocResponse.getAdmission().getStatus() != Admission.Status.ADMITTED) {
            throw new RuntimeException("Worker allocation failed: " + 
                allocResponse.getAdmission().getReason());
        }

        String workerId = allocResponse.getHandle().getWorkerId();
        log.info("Worker allocated: {}", workerId);

        try {
            // Wait for worker DNS to propagate and gRPC server to be ready
            Thread.sleep(1000);
            
            // Execute plugin via PPP
            CalculationResult result = executePlugin(
                pluginSpec,
                workerId,
                operation,
                request,
                requestId
            );
            
            return result;
        } finally {
            // Release worker
            runtimeClient.releaseWorker(
                pluginSpec.getRuntimeAddress(),
                workerId,
                "execution_complete"
            );
        }
    }

    private CalculationResult executePlugin(
            PluginRegistry.PluginSpec pluginSpec,
            String workerId,
            String operation,
            CalculationRequest request,
            String requestId) throws Exception {

        // Connect to worker via gRPC
        // For POC, we use the runtime address with worker-specific port offset
        String workerAddress = getWorkerAddress(pluginSpec.getRuntimeAddress(), workerId);
        
        ManagedChannel channel = ManagedChannelBuilder.forTarget(workerAddress)
            .usePlaintext()
            .build();

        try {
            ToolPluginGrpc.ToolPluginBlockingStub stub = ToolPluginGrpc.newBlockingStub(channel);

            // Initialize
            com.webex.agentic.proto.ppp.Context pppContext = 
                com.webex.agentic.proto.ppp.Context.newBuilder()
                    .setTenantId("demo-tenant")
                    .setUserId("demo-user")
                    .setSessionId(UUID.randomUUID().toString())
                    .setCorrelationId(requestId)
                    .build();

            InitRequest initReq = InitRequest.newBuilder()
                .setCtx(pppContext)
                .build();

            InitResponse initResp = stub.init(initReq);
            if (!initResp.getOk()) {
                throw new RuntimeException("Plugin init failed: " + initResp.getMessage());
            }

            // Invoke
            String jsonArgs = objectMapper.writeValueAsString(request);
            InvokeRequest invokeReq = InvokeRequest.newBuilder()
                .setCtx(pppContext)
                .setPrimitive(operation)
                .setVersion("1.0.0")
                .setArguments(Json.newBuilder()
                    .setValue(ByteString.copyFromUtf8(jsonArgs))
                    .build())
                .setRequestId(requestId)
                .build();

            Iterator<PluginMessage> responseStream = stub.invoke(invokeReq);
            
            CalculationResult result = null;
            while (responseStream.hasNext()) {
                PluginMessage message = responseStream.next();
                
                if (message.hasProgress()) {
                    log.info("Progress: {}%", message.getProgress().getPercent());
                } else if (message.hasCompleted()) {
                    String jsonOutput = message.getCompleted().getOutput().getValue().toStringUtf8();
                    result = objectMapper.readValue(jsonOutput, CalculationResult.class);
                    log.info("Plugin execution completed: {}", result);
                } else if (message.hasFailed()) {
                    throw new RuntimeException("Plugin execution failed: " + 
                        message.getFailed().getMessage());
                }
            }

            if (result == null) {
                throw new RuntimeException("No result received from plugin");
            }

            return result;

        } finally {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private String getWorkerAddress(String runtimeAddress, String workerId) {
        // Worker containers communicate via container name on same network
        // Worker exposes gRPC on port 8080 internally
        return workerId + ":8080";
    }
}

