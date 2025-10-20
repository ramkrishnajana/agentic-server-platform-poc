FROM python:3.11-slim

WORKDIR /app

# Copy proto files and generate Python code
COPY proto/src/main/proto/*.proto /app/proto/
RUN pip install grpcio-tools==1.60.0 && \
    python -m grpc_tools.protoc \
        -I/app/proto \
        --python_out=/app \
        --grpc_python_out=/app \
        /app/proto/*.proto

COPY plugins/python-plugins/divide/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY plugins/python-plugins/divide/divide_plugin.py .

EXPOSE 8080
CMD ["python", "divide_plugin.py"]

