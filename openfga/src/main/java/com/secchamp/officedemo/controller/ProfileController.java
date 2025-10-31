package com.secchamp.officedemo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secchamp.officedemo.service.OpenFgaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProfileController {
    
    private final OpenFgaService openFgaService;

    @GetMapping("/")
    public String index(Model model, Authentication authentication) {
        String username = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        model.addAttribute("currentUser", username);
        model.addAttribute("userRoles", roles);
        model.addAttribute("title", "User Profile - OpenFGA Demo");

        // Load user permissions and profile data from JSON
        try {
            Map<String, Object> userProfile = loadUserProfileFromJson(username);
            model.addAttribute("userProfile", userProfile);
            model.addAttribute("userPermissions", userProfile.get("permissions"));
            model.addAttribute("userDepartments", userProfile.get("departments"));
            model.addAttribute("userTeams", userProfile.get("teams"));
            model.addAttribute("accessibleResources", userProfile.get("accessibleResources"));
            
            // Load authorization model structure for matrix table
            Map<String, Object> matrixData = loadPermissionMatrixData();
            model.addAttribute("permissionMatrix", matrixData);
            
            // Create a permission matrix with pre-calculated checkbox states
            @SuppressWarnings("unchecked")
            List<Map<String, String>> permissions = (List<Map<String, String>>) userProfile.get("permissions");
            List<Map<String, Object>> permissionRows = createPermissionMatrixRows(permissions, matrixData);
            model.addAttribute("permissionRows", permissionRows);
            
        } catch (Exception e) {
            model.addAttribute("profileError", "Could not load user profile: " + e.getMessage());
        }

        return "index";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadUserProfileFromJson(String username) throws Exception {
        Map<String, Object> profile = new HashMap<>();
        
        // Load authorization model to get all possible resources and relations
        Map<String, Object> authModel = loadJsonFile("authorization_model.json");
        Map<String, Object> matrixData = loadPermissionMatrixData();
        List<Map<String, Object>> resourceInstances = (List<Map<String, Object>>) matrixData.get("resourceInstances");
        List<String> allRelations = (List<String>) matrixData.get("allRelations");
        
        String userPattern = "user:" + username + "@company.com";
        List<Map<String, String>> permissions = new ArrayList<>();
        Set<String> departments = new HashSet<>();
        Set<String> teams = new HashSet<>();
        List<Map<String, String>> accessibleResources = new ArrayList<>();
        
        // Check permissions for each resource and relation using OpenFGA API
        for (Map<String, Object> resource : resourceInstances) {
            String resourceId = (String) resource.get("id");
            String resourceType = (String) resource.get("type");
            
            for (String relation : allRelations) {
                // Only check relations that are valid for this resource type
                if (isValidRelationForResourceType(resourceType, relation, authModel)) {
                    boolean hasPermission = openFgaService.checkAccess(userPattern, relation, resourceId);
                    
                    if (hasPermission) {
                        Map<String, String> permission = new HashMap<>();
                        permission.put("relation", relation);
                        permission.put("object", resourceId);
                        permission.put("comment", "Checked via OpenFGA API");
                        permissions.add(permission);
                        
                        // Track departments and teams
                        if (resourceId.startsWith("department:")) {
                            departments.add(resourceId.substring(11));
                        }
                        if (resourceId.startsWith("team:")) {
                            teams.add(resourceId.substring(5));
                        }
                        
                        // Track accessible resources
                        Map<String, String> accessibleResource = new HashMap<>();
                        accessibleResource.put("type", resourceType);
                        accessibleResource.put("name", resourceId.split(":")[1]);
                        accessibleResource.put("permission", relation);
                        accessibleResources.add(accessibleResource);
                    }
                }
            }
        }
        
        profile.put("permissions", permissions);
        profile.put("departments", new ArrayList<>(departments));
        profile.put("teams", new ArrayList<>(teams));
        profile.put("accessibleResources", accessibleResources);
        
        // Determine user role and details
        Map<String, String> userDetails = determineUserDetails(username, permissions);
        profile.put("userDetails", userDetails);
        
        return profile;
    }
    
    @SuppressWarnings("unchecked")
    private boolean isValidRelationForResourceType(String resourceType, String relation, Map<String, Object> authModel) {
        List<Map<String, Object>> typeDefinitions = (List<Map<String, Object>>) authModel.get("type_definitions");
        
        for (Map<String, Object> typeDef : typeDefinitions) {
            if (resourceType.equals(typeDef.get("type"))) {
                Map<String, Object> relations = (Map<String, Object>) typeDef.get("relations");
                return relations != null && relations.containsKey(relation);
            }
        }
        return false;
    }
    
    private Map<String, String> determineUserDetails(String username, List<Map<String, String>> permissions) {
        Map<String, String> details = new HashMap<>();
        
        // Extract display name from username
        String displayName = Arrays.stream(username.split("[._]"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
        details.put("displayName", displayName);
        
        // Use the same role determination logic as the other method
        String userId = "user:" + username + "@company.com";
        List<Map<String, Object>> tuples = new ArrayList<>();
        
        // Convert permissions back to tuple format for consistent processing
        for (Map<String, String> permission : permissions) {
            Map<String, Object> tuple = new HashMap<>();
            tuple.put("user", userId);
            tuple.put("relation", permission.get("relation"));
            tuple.put("object", permission.get("object"));
            tuples.add(tuple);
        }
        
        String role = determineUserRoleFromTuples(userId, tuples);
        details.put("role", role);
        
        // Determine department from permissions
        String department = "";
        for (Map<String, String> permission : permissions) {
            String object = permission.get("object");
            if (object.startsWith("department:")) {
                department = formatDisplayName(object.substring(11)); // Remove "department:" prefix and format
                break; // Use first department found
            }
        }
        details.put("department", department);
        
        return details;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadJsonFile(String fileName) throws Exception {
        ClassPathResource resource = new ClassPathResource(fileName);
        ObjectMapper objectMapper = new ObjectMapper();
        
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, Map.class);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadPermissionMatrixData() throws Exception {
        Map<String, Object> matrixData = new HashMap<>();
        
        // Load authorization model to get resource types and relations
        Map<String, Object> authModel = loadJsonFile("authorization_model.json");
        List<Map<String, Object>> typeDefinitions = (List<Map<String, Object>>) authModel.get("type_definitions");
        
        // Extract all resource types (excluding 'user' type)
        List<Map<String, Object>> resourceTypes = new ArrayList<>();
        Set<String> allRelations = new LinkedHashSet<>();
        
        for (Map<String, Object> typeDef : typeDefinitions) {
            String typeName = (String) typeDef.get("type");
            if (!"user".equals(typeName)) { // Skip the base user type
                Map<String, Object> relations = (Map<String, Object>) typeDef.get("relations");
                if (relations != null) {
                    allRelations.addAll(relations.keySet());
                    
                    Map<String, Object> resourceType = new HashMap<>();
                    resourceType.put("type", typeName);
                    resourceType.put("displayName", formatResourceType(typeName));
                    resourceType.put("relations", new ArrayList<>(relations.keySet()));
                    resourceTypes.add(resourceType);
                }
            }
        }
        
        // Load initial data to get specific resource instances
        Map<String, Object> initialData = loadJsonFile("initial_data.json");
        List<Map<String, Object>> tuples = (List<Map<String, Object>>) initialData.get("tuples");
        
        // Extract unique resource instances from tuples
        Map<String, Map<String, Object>> resourceInstances = new LinkedHashMap<>();
        for (Map<String, Object> tuple : tuples) {
            String object = (String) tuple.get("object");
            if (object != null && object.contains(":")) {
                String[] parts = object.split(":", 2);
                String type = parts[0];
                String name = parts[1];
                
                if (!"user".equals(type) && !"department".equals(type)) { // Skip user and department types for matrix
                    String resourceKey = type + ":" + name;
                    if (!resourceInstances.containsKey(resourceKey)) {
                        Map<String, Object> resource = new HashMap<>();
                        resource.put("id", resourceKey);
                        resource.put("type", type);
                        resource.put("name", name);
                        resource.put("displayName", formatDisplayName(name));
                        resource.put("typeDisplayName", formatResourceType(type));
                        resourceInstances.put(resourceKey, resource);
                    }
                }
            }
        }
        
        // Add department instances as a special case
        for (Map<String, Object> tuple : tuples) {
            String object = (String) tuple.get("object");
            if (object != null && object.startsWith("department:")) {
                String[] parts = object.split(":", 2);
                String name = parts[1];
                String resourceKey = "department:" + name;
                
                if (!resourceInstances.containsKey(resourceKey)) {
                    Map<String, Object> resource = new HashMap<>();
                    resource.put("id", resourceKey);
                    resource.put("type", "department");
                    resource.put("name", name);
                    resource.put("displayName", formatDisplayName(name));
                    resource.put("typeDisplayName", "Department");
                    resourceInstances.put(resourceKey, resource);
                }
            }
        }
        
        // Add organization as a special case
        for (Map<String, Object> tuple : tuples) {
            String object = (String) tuple.get("object");
            if (object != null && object.startsWith("organization:")) {
                String[] parts = object.split(":", 2);
                String name = parts[1];
                String resourceKey = "organization:" + name;
                
                if (!resourceInstances.containsKey(resourceKey)) {
                    Map<String, Object> resource = new HashMap<>();
                    resource.put("id", resourceKey);
                    resource.put("type", "organization");
                    resource.put("name", name);
                    resource.put("displayName", formatDisplayName(name));
                    resource.put("typeDisplayName", "Organization");
                    resourceInstances.put(resourceKey, resource);
                }
            }
        }
        
        matrixData.put("resourceTypes", resourceTypes);
        matrixData.put("resourceInstances", new ArrayList<>(resourceInstances.values()));
        matrixData.put("allRelations", new ArrayList<>(allRelations));
        
        return matrixData;
    }
    
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> createPermissionMatrixRows(List<Map<String, String>> userPermissions, Map<String, Object> matrixData) {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> resourceInstances = (List<Map<String, Object>>) matrixData.get("resourceInstances");
        List<String> allRelations = (List<String>) matrixData.get("allRelations");
        
        for (Map<String, Object> resource : resourceInstances) {
            Map<String, Object> row = new HashMap<>();
            row.put("resource", resource);
            
            // Create a map of relation -> boolean for this resource
            Map<String, Boolean> relationPermissions = new HashMap<>();
            String resourceId = (String) resource.get("id");
            
            for (String relation : allRelations) {
                boolean hasPermission = userPermissions.stream()
                    .anyMatch(perm -> resourceId.equals(perm.get("object")) && relation.equals(perm.get("relation")));
                relationPermissions.put(relation, hasPermission);
            }
            
            row.put("permissions", relationPermissions);
            rows.add(row);
        }
        
        return rows;
    }

    private String formatDisplayName(String input) {
        return Arrays.stream(input.split("[._@]"))
                .filter(part -> !part.equals("company") && !part.equals("com"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }
    
    private String formatResourceType(String type) {
        switch (type) {
            case "confidential_info": return "Confidential Info";
            case "salary_info": return "Salary Info";
            default: return Arrays.stream(type.split("_"))
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                    .collect(Collectors.joining(" "));
        }
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