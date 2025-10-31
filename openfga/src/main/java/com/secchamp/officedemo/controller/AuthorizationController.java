package com.secchamp.officedemo.controller;

import com.secchamp.officedemo.service.OpenFgaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthorizationController {

    private final OpenFgaService openFgaService;

    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAccess(
            @RequestBody Map<String, String> request) {
        
        String userId = request.get("userId");
        String relation = request.get("relation");
        String objectId = request.get("objectId");
        
        log.info("Checking access: user={}, relation={}, object={}", userId, relation, objectId);
        
        boolean hasAccess = openFgaService.checkAccess(userId, relation, objectId);
        
        return ResponseEntity.ok(Map.of(
            "allowed", hasAccess,
            "userId", userId,
            "relation", relation,
            "objectId", objectId
        ));
    }

    @PostMapping("/grant")
    public ResponseEntity<Map<String, Object>> grantAccess(
            @RequestBody Map<String, String> request) {
        
        String userId = request.get("userId");
        String relation = request.get("relation");
        String objectId = request.get("objectId");
        
        try {
            openFgaService.grantAccess(userId, relation, objectId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Access granted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/revoke")
    public ResponseEntity<Map<String, Object>> revokeAccess(
            @RequestBody Map<String, String> request) {
        
        String userId = request.get("userId");
        String relation = request.get("relation");
        String objectId = request.get("objectId");
        
        try {
            openFgaService.revokeAccess(userId, relation, objectId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Access revoked successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}