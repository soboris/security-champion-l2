package com.secchamp.officedemo.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String id;
    private String email;
    private String name;
    private String department;
    private String role;
    private String managerId;
    private int salaryLevel;
    
    public enum Role {
        CEO("ceo"),
        DEPARTMENT_MANAGER("department_manager"),
        TEAM_LEAD("team_lead"),
        SENIOR_ENGINEER("senior_engineer"),
        ENGINEER("engineer"),
        HR_MANAGER("hr_manager"),
        HR_SPECIALIST("hr_specialist"),
        ADMIN("admin"),
        INTERN("intern");
        
        private final String value;
        
        Role(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public enum Department {
        ENGINEERING("engineering"),
        HR("hr"),
        FINANCE("finance"),
        MARKETING("marketing"),
        OPERATIONS("operations");
        
        private final String value;
        
        Department(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}