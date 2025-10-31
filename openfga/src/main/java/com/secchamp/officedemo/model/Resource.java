package com.secchamp.officedemo.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Resource {
    private String id;
    private String name;
    private String type;
    private String ownerId;
    private String department;
    private int confidentialityLevel;
    private String path;
    
    public enum ResourceType {
        DOCUMENT("document"),
        PROJECT("project"),
        SALARY_INFO("salary_info"),
        PERFORMANCE_REVIEW("performance_review"),
        SYSTEM_CONFIG("system_config"),
        FINANCIAL_REPORT("financial_report"),
        STRATEGIC_PLAN("strategic_plan"),
        EMPLOYEE_RECORD("employee_record");
        
        private final String value;
        
        ResourceType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public enum ConfidentialityLevel {
        PUBLIC(1),
        INTERNAL(2),
        CONFIDENTIAL(3),
        RESTRICTED(4),
        TOP_SECRET(5);
        
        private final int level;
        
        ConfidentialityLevel(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
}