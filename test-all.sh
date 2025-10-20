#!/bin/bash

echo "Testing Agentic Server Platform POC"
echo "===================================="
echo ""

# Wait for services to be ready
echo "Waiting for services to be ready..."
sleep 5

# Test Add (Java Plugin)
echo "1. Testing Add Operation (Java Plugin)"
echo "Request: 10 + 5"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/calculate/add \
  -H "Content-Type: application/json" \
  -d '{"operand1": 10, "operand2": 5}')
echo "Response: $RESPONSE"
echo ""

# Test Multiply (Java Plugin)
echo "2. Testing Multiply Operation (Java Plugin)"
echo "Request: 10 * 5"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/calculate/multiply \
  -H "Content-Type: application/json" \
  -d '{"operand1": 10, "operand2": 5}')
echo "Response: $RESPONSE"
echo ""

# Test Subtract (Python Plugin)
echo "3. Testing Subtract Operation (Python Plugin)"
echo "Request: 10 - 5"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/calculate/subtract \
  -H "Content-Type: application/json" \
  -d '{"operand1": 10, "operand2": 5}')
echo "Response: $RESPONSE"
echo ""

echo "===================================="
echo "All tests completed!"
echo ""
echo "Check docker ps to see if worker containers were created and destroyed:"
echo "docker ps -a | grep worker"

