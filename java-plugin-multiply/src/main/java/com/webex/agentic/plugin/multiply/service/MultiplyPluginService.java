package com.webex.agentic.plugin.multiply.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import com.webex.agentic.common.model.CalculationRequest;
import com.webex.agentic.common.model.CalculationResult;
import com.webex.agentic.proto.ppp.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * Multiply plugin - implements Platform-Plugin Protocol (PPP)
 */
@Slf4j
@GrpcService
public class MultiplyPluginService extends ToolPluginGrpc.ToolPluginImplBase {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init(InitRequest request, StreamObserver<InitResponse> responseObserver) {
        log.info("Plugin initialized for tenant: {}", request.getCtx().getTenantId());
        
        InitResponse response = InitResponse.newBuilder()
            .setOk(true)
            .setMessage("Multiply plugin ready")
            .putCaps("operation", "multiply")
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void invoke(InvokeRequest request, StreamObserver<PluginMessage> responseObserver) {
        log.info("Plugin invoked for primitive: {}", request.getPrimitive());
        
        try {
            // Parse input
            String jsonInput = request.getArguments().getValue().toStringUtf8();
            CalculationRequest calcRequest = objectMapper.readValue(jsonInput, CalculationRequest.class);
            
            log.info("Multiplying {} * {}", calcRequest.getOperand1(), calcRequest.getOperand2());

            // Send progress update
            PluginMessage progressMsg = PluginMessage.newBuilder()
                .setProgress(Progress.newBuilder()
                    .setRequestId(request.getRequestId())
                    .setTaskId(request.getRequestId())
                    .setPercent(50.0)
                    .setMessage("Performing multiplication...")
                    .setAt(Timestamp.newBuilder()
                        .setSeconds(System.currentTimeMillis() / 1000)
                        .build())
                    .build())
                .build();
            responseObserver.onNext(progressMsg);

            // Perform calculation
            double result = calcRequest.getOperand1() * calcRequest.getOperand2();
            
            CalculationResult calcResult = new CalculationResult(
                result,
                "multiply",
                calcRequest.getOperand1(),
                calcRequest.getOperand2()
            );

            // Send completion
            String jsonOutput = objectMapper.writeValueAsString(calcResult);
            PluginMessage completedMsg = PluginMessage.newBuilder()
                .setCompleted(Completed.newBuilder()
                    .setRequestId(request.getRequestId())
                    .setTaskId(request.getRequestId())
                    .setOutput(Json.newBuilder()
                        .setValue(ByteString.copyFromUtf8(jsonOutput))
                        .build())
                    .build())
                .build();
            
            responseObserver.onNext(completedMsg);
            responseObserver.onCompleted();
            
            log.info("Multiplication completed: {}", result);
            
        } catch (Exception e) {
            log.error("Error during plugin execution", e);
            
            PluginMessage failedMsg = PluginMessage.newBuilder()
                .setFailed(Failed.newBuilder()
                    .setRequestId(request.getRequestId())
                    .setTaskId(request.getRequestId())
                    .setCode("EXECUTION_ERROR")
                    .setMessage(e.getMessage())
                    .build())
                .build();
            
            responseObserver.onNext(failedMsg);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void health(Empty request, StreamObserver<InitResponse> responseObserver) {
        InitResponse response = InitResponse.newBuilder()
            .setOk(true)
            .setMessage("Healthy")
            .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

