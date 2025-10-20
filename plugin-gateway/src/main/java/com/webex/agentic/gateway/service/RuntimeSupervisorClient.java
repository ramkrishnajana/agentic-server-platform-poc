package com.webex.agentic.gateway.service;

import com.google.protobuf.Duration;
import com.webex.agentic.proto.supervisor.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Client to communicate with Runtime Supervisor services
 */
@Service
public class RuntimeSupervisorClient {
    
    private static final Logger log = LoggerFactory.getLogger(RuntimeSupervisorClient.class);

    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();
    private final Map<String, RuntimeSupervisorGrpc.RuntimeSupervisorBlockingStub> stubs = new ConcurrentHashMap<>();

    public AllocateWorkerResponse allocateWorker(String runtimeAddress, PluginRef plugin, Context context) {
        RuntimeSupervisorGrpc.RuntimeSupervisorBlockingStub stub = getStub(runtimeAddress);
        
        AllocateWorkerRequest request = AllocateWorkerRequest.newBuilder()
            .setPlugin(plugin)
            .setCtx(context)
            .setSoftDeadline(Duration.newBuilder().setSeconds(30).build())
            .setForceFreshProcess(true)  // POC: always create fresh worker
            .build();

        log.info("Allocating worker for plugin {} at {}", plugin.getId(), runtimeAddress);
        return stub.allocateWorker(request);
    }

    public ReleaseWorkerResponse releaseWorker(String runtimeAddress, String workerId, String reason) {
        RuntimeSupervisorGrpc.RuntimeSupervisorBlockingStub stub = getStub(runtimeAddress);
        
        ReleaseWorkerRequest request = ReleaseWorkerRequest.newBuilder()
            .setWorkerId(workerId)
            .setReason(reason)
            .build();

        log.info("Releasing worker {} at {} (reason: {})", workerId, runtimeAddress, reason);
        return stub.releaseWorker(request);
    }

    public HealthResponse health(String runtimeAddress) {
        RuntimeSupervisorGrpc.RuntimeSupervisorBlockingStub stub = getStub(runtimeAddress);
        return stub.health(HealthRequest.newBuilder().build());
    }

    private RuntimeSupervisorGrpc.RuntimeSupervisorBlockingStub getStub(String address) {
        return stubs.computeIfAbsent(address, addr -> {
            ManagedChannel channel = getChannel(addr);
            return RuntimeSupervisorGrpc.newBlockingStub(channel);
        });
    }

    private ManagedChannel getChannel(String address) {
        return channels.computeIfAbsent(address, addr -> {
            log.info("Creating gRPC channel to {}", addr);
            return ManagedChannelBuilder.forTarget(addr)
                .usePlaintext()
                .build();
        });
    }

    public void shutdown() {
        channels.values().forEach(channel -> {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Error shutting down channel", e);
            }
        });
    }
}

