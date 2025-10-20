package com.webex.agentic.runtime.java.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages worker container lifecycle
 */
@Service
public class WorkerManager {
    
    private static final Logger log = LoggerFactory.getLogger(WorkerManager.class);

    private final Map<String, WorkerProcess> workers = new ConcurrentHashMap<>();
    private final AtomicInteger portCounter = new AtomicInteger(10000);

    public WorkerProcess startWorker(String pluginId, String entrypoint) throws IOException {
        int port = portCounter.incrementAndGet();
        String workerId = "worker-" + port;
        
        log.info("Starting worker {} for plugin {} (entrypoint: {})", workerId, pluginId, entrypoint);

        // For POC, we use docker run to start worker containers
        String containerName = workerId;
        String imageName = getImageName(pluginId);
        
        ProcessBuilder pb = new ProcessBuilder(
            "docker", "run",
            "--name", containerName,
            "--network", "agentic-server-platform-poc_agentic-network",
            "-e", "WORKER_ID=" + workerId,
            "-e", "PLUGIN_ID=" + pluginId,
            "-d",  // detached mode
            "--rm", // auto-remove on stop
            imageName
        );
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Wait a bit for container to start
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        WorkerProcess worker = new WorkerProcess(workerId, pluginId, port, process, containerName);
        workers.put(workerId, worker);
        
        log.info("Worker {} started on port {}", workerId, port);
        return worker;
    }

    public void stopWorker(String workerId) {
        WorkerProcess worker = workers.remove(workerId);
        if (worker != null) {
            log.info("Stopping worker {}", workerId);
            try {
                // Stop docker container
                ProcessBuilder pb = new ProcessBuilder("docker", "stop", worker.getContainerName());
                pb.start().waitFor();
            } catch (Exception e) {
                log.error("Error stopping worker " + workerId, e);
            }
        }
    }

    private String getImageName(String pluginId) {
        return switch (pluginId) {
            case "add_numbers" -> "java-plugin-add:latest";
            case "multiply_numbers" -> "java-plugin-multiply:latest";
            default -> throw new IllegalArgumentException("Unknown plugin: " + pluginId);
        };
    }

    public static class WorkerProcess {
        private final String workerId;
        private final String pluginId;
        private final int port;
        private final Process process;
        private final String containerName;
        
        public WorkerProcess(String workerId, String pluginId, int port, Process process, String containerName) {
            this.workerId = workerId;
            this.pluginId = pluginId;
            this.port = port;
            this.process = process;
            this.containerName = containerName;
        }
        
        public String getWorkerId() {
            return workerId;
        }
        
        public String getPluginId() {
            return pluginId;
        }
        
        public int getPort() {
            return port;
        }
        
        public Process getProcess() {
            return process;
        }
        
        public String getContainerName() {
            return containerName;
        }
    }
}

