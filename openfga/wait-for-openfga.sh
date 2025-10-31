#!/bin/bash

echo "Waiting for OpenFGA to be ready..."
while ! curl -f http://openfga:8080/healthz > /dev/null 2>&1; do
  echo "OpenFGA not ready, waiting..."
  sleep 2
done

echo "OpenFGA is ready! Starting application..."
exec java -jar target/office-demo-0.0.1-SNAPSHOT.jar