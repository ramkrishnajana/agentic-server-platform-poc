FROM python:3.11-slim

# Install Docker CLI for managing worker containers
RUN apt-get update && \
    apt-get install -y docker.io && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy proto files and generate Python code
COPY proto/src/main/proto/*.proto /app/proto/
RUN pip install grpcio-tools==1.60.0 && \
    python -m grpc_tools.protoc \
        -I/app/proto \
        --python_out=/app \
        --grpc_python_out=/app \
        /app/proto/*.proto

COPY python-runtime-supervisor/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY python-runtime-supervisor/runtime_supervisor.py .

EXPOSE 9092
CMD ["python", "runtime_supervisor.py"]

