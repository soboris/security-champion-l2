package com.secchamp.officedemo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(Model model) {
        try {
            // Load demo users from initial_data.json
            List<Map<String, String>> demoUsers = loadDemoUsersFromJson();
            model.addAttribute("demoUsers", demoUsers);
        } catch (Exception e) {
            // Fallback to static data if JSON loading fails
            model.addAttribute("jsonError", "Could not load user data: " + e.getMessage());
        }
        return "login";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> loadDemoUsersFromJson() throws Exception {
        ClassPathResource resource = new ClassPathResource("initial_data.json");
        ObjectMapper objectMapper = new ObjectMapper();
        
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, Object> initialData = objectMapper.readValue(inputStream, Map.class);
            
            // First, try to load users from the "users" array in the JSON file
            List<Map<String, Object>> usersFromJson = (List<Map<String, Object>>) initialData.get("users");
            if (usersFromJson != null && !usersFromJson.isEmpty()) {
                List<Map<String, String>> usersList = new ArrayList<>();
                List<Map<String, Object>> tuples = (List<Map<String, Object>>) initialData.get("tuples");
                
                for (Map<String, Object> userDef : usersFromJson) {
                    Map<String, String> userInfo = new HashMap<>();
                    String username = (String) userDef.get("username");
                    String email = (String) userDef.get("email");
                    String displayName = (String) userDef.get("displayName");
                    
                    userInfo.put("userId", email);
                    userInfo.put("username", username);
                    userInfo.put("displayName", displayName != null ? displayName : formatDisplayName(username));
                    
                    // Determine role for this user using the same logic as DemoController
                    String userId = "user:" + email;
                    String role = determineUserRoleFromTuples(userId, tuples);
                    userInfo.put("roles", role);
                    
                    usersList.add(userInfo);
                }
                
                return usersList.stream()
                        .sorted(Comparator.comparing(u -> u.get("displayName")))
                        .collect(Collectors.toList());
            }
            
            // Fallback: Extract unique users and their roles from tuples (for backward compatibility)
            List<Map<String, Object>> tuples = (List<Map<String, Object>>) initialData.get("tuples");
            Map<String, Map<String, String>> userMap = new HashMap<>();
            
            for (Map<String, Object> tuple : tuples) {
                String user = (String) tuple.get("user");
                String relation = (String) tuple.get("relation");
                String object = (String) tuple.get("object");
                
                if (user != null && user.startsWith("user:")) {
                    String userId = user.substring(5); // Remove "user:" prefix
                    String username = userId.split("@")[0];
                    String displayName = formatDisplayName(username);
                    
                    if (!userMap.containsKey(userId)) {
                        Map<String, String> userInfo = new HashMap<>();
                        userInfo.put("userId", userId);
                        userInfo.put("username", username);
                        userInfo.put("displayName", displayName);
                        userInfo.put("roles", "");
                        userMap.put(userId, userInfo);
                    }
                    
                    // Determine role based on relations and objects
                    String role = determineUserRole(relation, object, userId);
                    if (!role.isEmpty()) {
                        Map<String, String> userInfo = userMap.get(userId);
                        String existingRoles = userInfo.get("roles");
                        if (existingRoles.isEmpty()) {
                            userInfo.put("roles", role);
                        } else if (!existingRoles.contains(role)) {
                            userInfo.put("roles", existingRoles + ", " + role);
                        }
                    }
                }
            }
            
            return userMap.values().stream()
                    .sorted(Comparator.comparing(u -> u.get("displayName")))
                    .collect(Collectors.toList());
        }
    }
    
    private String formatDisplayName(String username) {
        return Arrays.stream(username.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }
    
    private String determineUserRole(String relation, String object, String userId) {
        if ("owner".equals(relation) && object.contains("organization:")) {
            return "CEO";
        } else if ("director".equals(relation) && object.contains("department:engineering")) {
            return "CTO";
        } else if ("director".equals(relation) && object.contains("department:")) {
            return "Director";
        } else if ("manager".equals(relation) && object.contains("department:")) {
            return "Manager";
        } else if ("lead".equals(relation) && object.contains("team:")) {
            return "Team Lead";
        } else if ("member".equals(relation) && object.contains("department:")) {
            return "Employee";
        }
        return "";
    }

    private String determineUserRoleFromTuples(String userId, List<Map<String, Object>> tuples) {
        // Special case: john_doe is always CEO since the OpenFgaService grants him owner access to everything
        if ("user:john_doe@company.com".equals(userId)) {
            return "Chief Executive Officer";
        }
        
        Set<String> roles = new HashSet<>();
        
        for (Map<String, Object> tuple : tuples) {
            String tupleUser = (String) tuple.get("user");
            String tupleRelation = (String) tuple.get("relation");
            String tupleObject = (String) tuple.get("object");
            
            if (userId.equals(tupleUser)) {
                if ("owner".equals(tupleRelation) && tupleObject.contains("organization:")) {
                    roles.add("CEO");
                } else if ("director".equals(tupleRelation) && tupleObject.contains("department:engineering")) {
                    roles.add("CTO");
                } else if ("director".equals(tupleRelation)) {
                    roles.add("Director");
                } else if ("manager".equals(tupleRelation)) {
                    roles.add("Manager");
                } else if ("lead".equals(tupleRelation)) {
                    roles.add("Team Lead");
                } else if ("member".equals(tupleRelation)) {
                    roles.add("Employee");
                }
            }
        }
        
        // Return highest priority role
        if (roles.contains("CEO")) return "Chief Executive Officer";
        if (roles.contains("CTO")) return "Chief Technology Officer";
        if (roles.contains("Director")) return "Director";
        if (roles.contains("Manager")) return "Manager";
        if (roles.contains("Team Lead")) return "Team Lead";
        if (roles.contains("Employee")) return "Employee";
        
        return "Unknown Role";
    }
}