package com.cybersecurity.sechamp2025.controllers.api;

import com.cybersecurity.sechamp2025.models.User;
import com.cybersecurity.sechamp2025.services.UserService;
import com.cybersecurity.sechamp2025.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
public class UserApiController {

    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7); // Remove "Bearer "
        String userId = JwtUtil.validateAndExtractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Invalid or expired token"));
        }

        // Return only the current authenticated user, not all users
        User currentUser = userService.findById(userId);
        if (currentUser == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        // Return as array for compatibility with frontend that expects data[0]
        return ResponseEntity.ok(Arrays.asList(currentUser));
    }

    
    @GetMapping("/users/{userId}/credits")
    public ResponseEntity<?> getUserCredits(@PathVariable String userId) {
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("name", user.getName());
        response.put("creditLimit", user.getCreditLimit());
        response.put("creditLimitAsDouble", user.getCreditLimitAsDouble());
        
        return ResponseEntity.ok(response);
    }

    
    @PostMapping("/users/{userId}/credits")
    public ResponseEntity<?> modifyUserCredits(@PathVariable String userId, @RequestBody Map<String, Object> requestBody) {
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        try {
            if (requestBody.containsKey("creditLimit")) {
                Object creditLimitObj = requestBody.get("creditLimit");
                BigDecimal newCreditLimit;
                
                if (creditLimitObj instanceof Number) {
                    newCreditLimit = BigDecimal.valueOf(((Number) creditLimitObj).doubleValue());
                } else if (creditLimitObj instanceof String) {
                    newCreditLimit = new BigDecimal((String) creditLimitObj);
                } else {
                    return ResponseEntity.status(400).body(Map.of("error", "Invalid credit limit format"));
                }
                
                user.setCreditLimit(newCreditLimit);
                userService.updateUser(user);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Credit limit updated successfully");
                response.put("userId", user.getId());
                response.put("newCreditLimit", user.getCreditLimit());
                response.put("newCreditLimitAsDouble", user.getCreditLimitAsDouble());
                
                return ResponseEntity.ok(response);
            }
            
            if (requestBody.containsKey("addCredits")) {
                Object addCreditsObj = requestBody.get("addCredits");
                BigDecimal creditsToAdd;
                
                if (addCreditsObj instanceof Number) {
                    creditsToAdd = BigDecimal.valueOf(((Number) addCreditsObj).doubleValue());
                } else if (addCreditsObj instanceof String) {
                    creditsToAdd = new BigDecimal((String) addCreditsObj);
                } else {
                    return ResponseEntity.status(400).body(Map.of("error", "Invalid credits to add format"));
                }
                
                BigDecimal currentCredits = user.getCreditLimit();
                BigDecimal newCredits = currentCredits.add(creditsToAdd);
                user.setCreditLimit(newCredits);
                userService.updateUser(user);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Credits added successfully");
                response.put("userId", user.getId());
                response.put("creditsAdded", creditsToAdd);
                response.put("previousCredits", currentCredits);
                response.put("newCreditLimit", user.getCreditLimit());
                response.put("newCreditLimitAsDouble", user.getCreditLimitAsDouble());
                
                return ResponseEntity.ok(response);
            }
            
            return ResponseEntity.status(400).body(Map.of("error", "No valid operation specified. Use 'creditLimit' to set or 'addCredits' to add"));
            
        } catch (NumberFormatException e) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid number format"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable String userId) {
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        // Return all user columns - publicly accessible
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("phone", user.getPhone());
        response.put("email", user.getEmail());
        response.put("address", user.getAddress());
        response.put("role", user.getRole());
        response.put("isAdmin", user.isAdmin());
        response.put("accountStatus", user.getAccountStatus());
        response.put("creditLimit", user.getCreditLimit());
        response.put("creditLimitAsDouble", user.getCreditLimitAsDouble());
        response.put("newsletter", user.isNewsletter());
        response.put("promotions", user.isPromotions());
        
        return ResponseEntity.ok(response);
    }
}
