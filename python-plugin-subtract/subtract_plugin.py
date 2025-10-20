#!/usr/bin/env python3
"""
Subtract Plugin - implements Platform-Plugin Protocol (PPP)
"""

import json
import logging
import time
from concurrent import futures
import sys
import os

import grpc

# Add proto path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'proto', 'target', 'generated-sources', 'protobuf', 'python'))

from google.protobuf import empty_pb2, timestamp_pb2
from plugin_protocol_pb2 import *
from plugin_protocol_pb2_grpc import ToolPluginServicer, add_ToolPluginServicer_to_server

logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class SubtractPlugin(ToolPluginServicer):
    
    def Init(self, request, context):
        logger.info(f"Plugin initialized for tenant: {request.ctx.tenant_id}")
        
        response = InitResponse()
        response.ok = True
        response.message = "Subtract plugin ready"
        response.caps["operation"] = "subtract"
        
        return response
    
    def Invoke(self, request, context):
        logger.info(f"Plugin invoked for primitive: {request.primitive}")
        
        try:
            # Parse input
            json_input = request.arguments.value.decode('utf-8')
            calc_request = json.loads(json_input)
            
            operand1 = calc_request['operand1']
            operand2 = calc_request['operand2']
            
            logger.info(f"Subtracting {operand1} - {operand2}")
            
            # Send progress update
            progress_msg = PluginMessage()
            progress_msg.progress.request_id = request.request_id
            progress_msg.progress.task_id = request.request_id
            progress_msg.progress.percent = 50.0
            progress_msg.progress.message = "Performing subtraction..."
            progress_msg.progress.at.seconds = int(time.time())
            yield progress_msg
            
            # Perform calculation
            result = operand1 - operand2
            
            calc_result = {
                "result": result,
                "operation": "subtract",
                "operand1": operand1,
                "operand2": operand2
            }
            
            # Send completion
            json_output = json.dumps(calc_result)
            completed_msg = PluginMessage()
            completed_msg.completed.request_id = request.request_id
            completed_msg.completed.task_id = request.request_id
            completed_msg.completed.output.value = json_output.encode('utf-8')
            yield completed_msg
            
            logger.info(f"Subtraction completed: {result}")
            
        except Exception as e:
            logger.error(f"Error during plugin execution: {e}", exc_info=True)
            
            failed_msg = PluginMessage()
            failed_msg.failed.request_id = request.request_id
            failed_msg.failed.task_id = request.request_id
            failed_msg.failed.code = "EXECUTION_ERROR"
            failed_msg.failed.message = str(e)
            yield failed_msg
    
    def Health(self, request, context):
        response = InitResponse()
        response.ok = True
        response.message = "Healthy"
        return response


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    add_ToolPluginServicer_to_server(SubtractPlugin(), server)
    server.add_insecure_port('[::]:8080')
    
    logger.info("Subtract Plugin starting on port 8080")
    server.start()
    
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        logger.info("Shutting down...")
        server.stop(0)


if __name__ == '__main__':
    serve()

