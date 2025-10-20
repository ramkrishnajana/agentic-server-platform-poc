package com.webex.agentic.runtime.java.grpc;

import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.webex.agentic.proto.supervisor.*;
import com.webex.agentic.runtime.java.service.WorkerManager;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC service implementation for Runtime Supervisor API
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RuntimeSupervisorService extends RuntimeSupervisorGrpc.RuntimeSupervisorImplBase {

    private final WorkerManager workerManager;

    @Override
    public void ensurePlugin(EnsurePluginRequest request, StreamObserver<EnsurePluginResponse> responseObserver) {
        log.info("EnsurePlugin called for: {}", request.getPlugin().getId());
        
        EnsurePluginResponse response = EnsurePluginResponse.newBuilder()
            .setState(EnsurePluginResponse.State.READY)
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void allocateWorker(AllocateWorkerRequest request, StreamObserver<AllocateWorkerResponse> responseObserver) {
        log.info("AllocateWorker called for: {}", request.getPlugin().getId());
        
        try {
            WorkerManager.WorkerProcess worker = workerManager.startWorker(
                request.getPlugin().getId(),
                request.getPlugin().getEntrypoint()
            );

            WorkerHandle handle = WorkerHandle.newBuilder()
                .setWorkerId(worker.getWorkerId())
                .setRuntime("java")
                .setNotBefore(Timestamp.newBuilder()
                    .setSeconds(System.currentTimeMillis() / 1000)
                    .build())
                .build();

            Admission admission = Admission.newBuilder()
                .setStatus(Admission.Status.ADMITTED)
                .build();

            AllocateWorkerResponse response = AllocateWorkerResponse.newBuilder()
                .setAdmission(admission)
                .setHandle(handle)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error allocating worker", e);
            
            Admission admission = Admission.newBuilder()
                .setStatus(Admission.Status.REJECTED)
                .setReason("Failed to start worker: " + e.getMessage())
                .build();

            AllocateWorkerResponse response = AllocateWorkerResponse.newBuilder()
                .setAdmission(admission)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void releaseWorker(ReleaseWorkerRequest request, StreamObserver<ReleaseWorkerResponse> responseObserver) {
        log.info("ReleaseWorker called for: {}", request.getWorkerId());
        
        workerManager.stopWorker(request.getWorkerId());
        
        ReleaseWorkerResponse response = ReleaseWorkerResponse.newBuilder()
            .setStats(Struct.newBuilder().build())
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void health(HealthRequest request, StreamObserver<HealthResponse> responseObserver) {
        log.debug("Health check called");
        
        HealthResponse response = HealthResponse.newBuilder()
            .setStatus(HealthResponse.Status.OK)
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

