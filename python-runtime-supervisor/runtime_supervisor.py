#!/usr/bin/env python3
"""
Python Runtime Supervisor
Manages Python plugin worker lifecycle
"""

import logging
import subprocess
import time
from concurrent import futures
from typing import Dict

import grpc
import sys
import os

# Add proto path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'proto', 'target', 'generated-sources', 'protobuf', 'python'))

from google.protobuf import timestamp_pb2, duration_pb2, struct_pb2
from runtime_supervisor_pb2 import *
from runtime_supervisor_pb2_grpc import RuntimeSupervisorServicer, add_RuntimeSupervisorServicer_to_server

logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class WorkerProcess:
    def __init__(self, worker_id: str, plugin_id: str, port: int, container_name: str):
        self.worker_id = worker_id
        self.plugin_id = plugin_id
        self.port = port
        self.container_name = container_name


class PythonRuntimeSupervisor(RuntimeSupervisorServicer):
    def __init__(self):
        self.workers: Dict[str, WorkerProcess] = {}
        self.port_counter = 20000

    def EnsurePlugin(self, request, context):
        logger.info(f"EnsurePlugin called for: {request.plugin.id}")
        
        response = EnsurePluginResponse()
        response.state = EnsurePluginResponse.READY
        return response

    def AllocateWorker(self, request, context):
        logger.info(f"AllocateWorker called for: {request.plugin.id}")
        
        try:
            # Allocate port
            self.port_counter += 1
            port = self.port_counter
            worker_id = f"worker-{port}"
            container_name = worker_id
            
            # Start docker container
            image_name = self._get_image_name(request.plugin.id)
            
            cmd = [
                "docker", "run",
                "--name", container_name,
                "--network", "agentic-server-platform-poc_agentic-network",
                "-e", f"WORKER_ID={worker_id}",
                "-e", f"PLUGIN_ID={request.plugin.id}",
                "-d",
                "--rm",
                image_name
            ]
            
            logger.info(f"Starting worker with command: {' '.join(cmd)}")
            subprocess.run(cmd, check=True, capture_output=True, text=True)
            
            # Wait for container to start
            time.sleep(2)
            
            # Store worker info
            worker = WorkerProcess(worker_id, request.plugin.id, port, container_name)
            self.workers[worker_id] = worker
            
            logger.info(f"Worker {worker_id} started on port {port}")
            
            # Build response
            response = AllocateWorkerResponse()
            response.admission.status = Admission.ADMITTED
            
            response.handle.worker_id = worker_id
            response.handle.runtime = "python"
            response.handle.not_before.seconds = int(time.time())
            
            return response
            
        except Exception as e:
            logger.error(f"Error allocating worker: {e}", exc_info=True)
            
            response = AllocateWorkerResponse()
            response.admission.status = Admission.REJECTED
            response.admission.reason = f"Failed to start worker: {str(e)}"
            return response

    def ReleaseWorker(self, request, context):
        logger.info(f"ReleaseWorker called for: {request.worker_id}")
        
        worker = self.workers.pop(request.worker_id, None)
        if worker:
            try:
                # Stop docker container
                subprocess.run(
                    ["docker", "stop", worker.container_name],
                    check=True,
                    capture_output=True,
                    text=True,
                    timeout=10
                )
                logger.info(f"Worker {request.worker_id} stopped")
            except Exception as e:
                logger.error(f"Error stopping worker {request.worker_id}: {e}")
        
        response = ReleaseWorkerResponse()
        return response

    def Health(self, request, context):
        logger.debug("Health check called")
        
        response = HealthResponse()
        response.status = HealthResponse.OK
        return response

    def _get_image_name(self, plugin_id: str) -> str:
        if plugin_id == "subtract_numbers":
            return "python-plugin-subtract:latest"
        else:
            raise ValueError(f"Unknown plugin: {plugin_id}")


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    add_RuntimeSupervisorServicer_to_server(PythonRuntimeSupervisor(), server)
    server.add_insecure_port('[::]:9092')
    
    logger.info("Python Runtime Supervisor starting on port 9092")
    server.start()
    
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        logger.info("Shutting down...")
        server.stop(0)


if __name__ == '__main__':
    serve()

