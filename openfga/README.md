# Showcasing Relationship-Based Access Control (ReBAC)

A comprehensive demonstration of OpenFGA (Open Fine-Grained Authorization) implementation in a Java Spring Boot application.

## Getting Started

### Prerequisites
- Docker Desktop installed and running
- Docker Compose available

### Quick Start

1. **Navigate to project directory:**
   ```bash
   cd openfga_demo
   ```

2. **Start all services:**
   ```bash
   docker-compose up --build
   ```

3. **Wait for services to initialize** (approximately 30-60 seconds)

4. **Access the applications:**
   - **Web Demo Interface**: http://localhost:8090
   - **OpenFGA Playground**: http://localhost:3000/playground
   - **OpenFGA API**: http://localhost:8080


## How to Use the Demo

### Web Interface Testing

1. **Web Demo:**
   - The main page at http://localhost:8090 provides a web form for viewing permissions
   - login as different users and view the permission matrix

3. **OpenFGA Playground**: http://localhost:3000/playground
   - Visual interface for exploring the authorization model
   - Test relationships and permissions interactively
   - View the authorization model 
   
4. **OpenFGA API**: http://localhost:8080
   - refer to next section

## API Usage

## Direct OpenFGA API Usage (Port 8080)

### Getting Store ID and Authorization Model ID

Before using the direct OpenFGA API, you need to retrieve the Store ID and Authorization Model ID that are dynamically created by the application:

**Get Store ID:**
```bash
curl -X GET http://localhost:8080/stores
```

response
```
{"stores":[{"id":"01K8T23E2ZVT8RW7MX232S41H9", "name":"Office Demo Store", "created_at":"2025-10-30T08:00:16.479145382Z", "updated_at":"2025-10-30T08:00:16.479145382Z", "deleted_at":null}], "continuation_token":""}
```

**Get Authorization Model ID (after getting Store ID):**
```bash
curl -X GET "http://localhost:8080/stores/{STORE_ID}/authorization-models"

curl -X GET "http://localhost:8080/stores/01K8T23E2ZVT8RW7MX232S41H9/authorization-models"

```

response
```
{"authorization_models":[{"id":"01K8T23E551Q23QERD428063WY", "schema_version":"1.1", "type_definitions":[{"type":"user", "relations":{}, "metadata":null}, {"type":"organization", "relations":{"member":{"union":{"child":[{"this":{}}, {"computedUserset":{"object":"", "relation":"owner"}}]}}, "owner":{"this":{}}}, "metadata":{"relations":{"member":{"directly_related_user_types":[{"type":"user", "condition":""}], "module":"", "source_info":null}, "owner":{"directly_related_user_types":[{"type":"user", "condition":""}], "module":"", "source_info":null}}, "module":"", "source_info":null}}, {"type":"department", "relations":{"manager":{"union":{"child":[{"this":{}}, {"computedUserset":{"object":"", "relation":"owner"}}]}}, "member":{"union":{"child":[{"this":{}}, {"computedUserset":{"object":"", "relation":"manager"}}, {"computedUserset":{"object":"", "relation":"owner"}}]}}, "owner":{"this":{}}}, "metadata":{"relations":{"manager":{"directly_related_user_types":[{"type":"user", "condition":""}], "module":"", "source_info":null}, "member":{"directly_related_user_types":[{"type":"user", "condition":""}], "module":"", "source_info":null}, "owner":{"directly_related_user_types":[{"type":"user", "condition":""}], "module":"", "source_info":null}}, "module":"", "source_info":null}}, {"type":"documents", "relations":{"owner":{"this":{}}, "viewer":{"this":{}}}, "metadata":{"relations":{"owner":{"directly_related_user_types":[{"type":"user", "condition":""}, {"type":"department", "condition":""}], "module":"", "source_info":null}, "viewer":{"directly_related_user_types":[{"type":"user", "condition":""}], "module":"", "source_info":null}}, "module":"", "source_info":null}}, {"type":"IT_resources", "relations":{"it-admin":{"this":{}}, "owner":{"this":{}}}, "metadata":{"relations":{"it-admin":{"directly_related_user_types":[{"type":"user", "condition":""}], "module":"", "source_info":null}, "owner":{"directly_related_user_types":[{"type":"user", "condition":""}, {"type":"department", "condition":""}], "module":"", "source_info":null}}, "module":"", "source_info":null}}], "conditions":{}}], "continuation_token":""}
```

### Direct OpenFGA API Examples (using the store and model ID above)

**Check Authorization (using correct tuple format):**
```bash
curl -X POST "http://localhost:8080/stores/01K8T23E2ZVT8RW7MX232S41H9/check" \
  -H "Content-Type: application/json" \
  -d '{
    "authorization_model_id": "01K8T23E551Q23QERD428063WY",
    "tuple_key": {
      "user": "user:it_manager@company.com",
      "relation": "it-admin",
      "object": "IT_resources:company_servers"
    }
  }'
```

response
```
{"allowed":true, "resolution":""}
```

**Read Relationship Tuples:**
```bash
curl -X POST "http://localhost:8080/stores/01K8T23E2ZVT8RW7MX232S41H9/read" \
  -H "Content-Type: application/json" \
  -d '{
    "tuple_key": {
      "user": "user:john_doe@company.com",
      "relation": "owner",
      "object": "organization:company"
    }
  }'
```

response
```
{"tuples":[{"key":{"user":"user:john_doe@company.com", "relation":"owner", "object":"organization:company", "condition":null}, "timestamp":"2025-10-30T08:00:16.561264837Z"}], "continuation_token":""}
```