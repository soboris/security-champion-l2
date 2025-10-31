<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>

<!DOCTYPE html>
<html>
<head>
    <title>Admin Panel</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
        .container { max-width: 800px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
        .admin-section { background: #ffe6e6; padding: 15px; border-radius: 5px; margin: 10px 0; border: 2px solid #ff9999; }
        .warning { background: #fff3cd; color: #856404; padding: 10px; border-radius: 4px; margin: 10px 0; }
    </style>
</head>
<body>
    <div class="container">
        <h2>Admin Panel</h2>
        
        <div class="warning">
            <strong>Warning:</strong> This admin panel was discovered through JavaScript file analysis!
        </div>
        
        <div class="admin-section">
            <h3>File Management</h3>
            <p>Administrative file operations:</p>
            <ul>
                <li><a href="search.jsp?ACTION=DELETE&fileName=*">Delete All Files</a> (Dangerous!)</li>
                <li><a href="search.jsp">File Upload Interface</a></li>
            </ul>
            
            <h3>System Information</h3>
            <p>Server Path: <%= application.getRealPath("") %></p>
            <p>Session ID: <%= session.getId() %></p>
            <p>Current Time: <%= new Date() %></p>
        </div>
        
        <div class="admin-section">
            <h3>Quick File Operations</h3>
            <form action="search.jsp" method="get">
                <input type="hidden" name="ACTION" value="DELETE">
                <p>Delete File: <input type="text" name="fileName" placeholder="filename.ext"></p>
                <p><input type="submit" value="Delete" style="background: #e74c3c; color: white; padding: 8px 16px; border: none; border-radius: 4px;"></p>
            </form>
        </div>
        
        <p><a href="search.jsp">Back to Search</a></p>
    </div>
</body>
</html>