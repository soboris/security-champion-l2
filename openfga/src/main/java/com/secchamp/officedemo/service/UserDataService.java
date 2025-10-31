package com.secchamp.officedemo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserDataService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<UserDetails> loadUsersFromInitialData() {
        List<UserDetails> users = new ArrayList<>();
        
        try {
            ClassPathResource resource = new ClassPathResource("initial_data.json");
            JsonNode rootNode = objectMapper.readTree(resource.getInputStream());
            JsonNode usersNode = rootNode.get("users");
            
            if (usersNode != null && usersNode.isArray()) {
                for (JsonNode userNode : usersNode) {
                    String username = userNode.get("username").asText();
                    String password = userNode.get("password").asText();
                    
                    List<String> rolesList = new ArrayList<>();
                    JsonNode rolesNode = userNode.get("roles");
                    if (rolesNode != null && rolesNode.isArray()) {
                        for (JsonNode roleNode : rolesNode) {
                            rolesList.add(roleNode.asText());
                        }
                    }
                    
                    UserDetails user = User.builder()
                            .username(username)
                            .password("{noop}" + password)
                            .roles(rolesList.toArray(new String[0]))
                            .build();
                    
                    users.add(user);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load users from initial_data.json", e);
        }
        
        return users;
    }
    
    public JsonNode loadInitialDataJson() {
        try {
            ClassPathResource resource = new ClassPathResource("initial_data.json");
            return objectMapper.readTree(resource.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load initial_data.json", e);
        }
    }
}