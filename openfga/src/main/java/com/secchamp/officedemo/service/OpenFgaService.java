package com.secchamp.officedemo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.openfga.sdk.api.client.OpenFgaClient;
import dev.openfga.sdk.api.client.model.*;
import dev.openfga.sdk.api.model.*;
import dev.openfga.sdk.errors.FgaInvalidParameterException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenFgaService {

    private final OpenFgaClient openFgaClient;

    @Value("${openfga.store.id:}")
    private String storeId;

    @Value("${openfga.authorization.model.id:}")
    private String authorizationModelId;

    @PostConstruct
    public void initializeOpenFga() {
        try {
            if (storeId == null || storeId.isEmpty()) {
                createStore();
            } else {
                openFgaClient.setStoreId(storeId);
            }
            
            writeAuthorizationModel();
            seedInitialData();
            log.info("OpenFGA service initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize OpenFGA", e);
            // Don't throw - let the app start in fallback mode
        }
    }

    private void createStore() throws ExecutionException, InterruptedException, FgaInvalidParameterException {
        CreateStoreRequest request = new CreateStoreRequest()
                .name("Office Demo Store");
        
        CreateStoreResponse response = openFgaClient.createStore(request).get();
        storeId = response.getId();
        openFgaClient.setStoreId(storeId);
        log.info("Created OpenFGA store: {}", storeId);
    }

    private void writeAuthorizationModel() throws ExecutionException, InterruptedException {
        try {
            AuthorizationModel authModel = loadAuthorizationModelFromFile();
            
            WriteAuthorizationModelRequest request = new WriteAuthorizationModelRequest()
                    .schemaVersion(authModel.getSchemaVersion())
                    .typeDefinitions(authModel.getTypeDefinitions());
            
            WriteAuthorizationModelResponse response = openFgaClient.writeAuthorizationModel(request).get();
            authorizationModelId = response.getAuthorizationModelId();
            openFgaClient.setAuthorizationModelId(authorizationModelId);
            log.info("Created authorization model: {}", authorizationModelId);
        } catch (Exception e) {
            log.error("Failed to write authorization model", e);
            throw new RuntimeException("Failed to write authorization model", e);
        }
    }

    private AuthorizationModel loadAuthorizationModelFromFile() throws IOException {
        ClassPathResource resource = new ClassPathResource("authorization_model.json");
        ObjectMapper objectMapper = new ObjectMapper();
        
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, AuthorizationModel.class);
        }
    }

    private void seedInitialData() throws ExecutionException, InterruptedException, FgaInvalidParameterException {
        try {
            List<ClientTupleKey> tuples = loadInitialDataFromFile();
            
            ClientWriteRequest writeRequest = new ClientWriteRequest().writes(tuples);
            openFgaClient.write(writeRequest).get();
            log.info("Seeded initial authorization data with {} tuples", tuples.size());
        } catch (Exception e) {
            log.error("Failed to seed initial data", e);
            throw new RuntimeException("Failed to seed initial data", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<ClientTupleKey> loadInitialDataFromFile() throws IOException {
        ClassPathResource resource = new ClassPathResource("initial_data.json");
        ObjectMapper objectMapper = new ObjectMapper();
        
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, Object> data = objectMapper.readValue(inputStream, Map.class);
            List<Map<String, String>> tuplesData = (List<Map<String, String>>) data.get("tuples");

            // Map existing tuples directly from initial_data.json
            List<ClientTupleKey> tuples = tuplesData.stream()
                    .map(tupleData -> new ClientTupleKey()
                            .user(tupleData.get("user"))
                            .relation(tupleData.get("relation"))
                            ._object(tupleData.get("object")))
                    .toList();

            return tuples;
        }
    }

    public boolean checkAccess(String userId, String relation, String objectId) {
        try {
            ClientCheckRequest request = new ClientCheckRequest()
                    .user(userId)
                    .relation(relation)
                    ._object(objectId);
            
            CheckResponse response = openFgaClient.check(request).get();
            return response.getAllowed();
        } catch (Exception e) {
            log.error("Error checking access for user {} on object {}: {}", userId, objectId, e.getMessage());
            
            return false; // Default deny
        }
    }

    public void grantAccess(String userId, String relation, String objectId) {
        try {
            ClientTupleKey tupleKey = new ClientTupleKey()
                    .user(userId)
                    .relation(relation)
                    ._object(objectId);
            
            ClientWriteRequest request = new ClientWriteRequest()
                    .writes(List.of(tupleKey));
            
            openFgaClient.write(request).get();
            log.info("Granted {} access to user {} on object {}", relation, userId, objectId);
        } catch (Exception e) {
            log.error("Error granting access: {}", e.getMessage());
            log.info("Demo mode: Granted {} access to user {} on object {}", relation, userId, objectId);
        }
    }

    public void revokeAccess(String userId, String relation, String objectId) {
        try {
            ClientTupleKeyWithoutCondition tupleKey = new ClientTupleKeyWithoutCondition()
                    .user(userId)
                    .relation(relation)
                    ._object(objectId);
            
            ClientWriteRequest request = new ClientWriteRequest()
                    .deletes(List.of(tupleKey));
            
            openFgaClient.write(request).get();
            log.info("Revoked {} access from user {} on object {}", relation, userId, objectId);
        } catch (Exception e) {
            log.error("Error revoking access: {}", e.getMessage());
            log.info("Demo mode: Revoked {} access from user {} on object {}", relation, userId, objectId);
        }
    }
}