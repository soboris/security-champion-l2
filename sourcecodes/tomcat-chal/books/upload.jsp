<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="javax.servlet.http.Part" %>

<!DOCTYPE html>
<html>
<head>
    <title>File Upload Handler</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
        .container { max-width: 600px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
        .form-section { background: #e8f5e8; padding: 15px; border-radius: 5px; margin: 10px 0; }
    </style>
</head>
<body>
    <div class="container">
        <h2>Simple File Upload</h2>
        <p>This page provides direct file upload functionality.</p>
        
        <div class="form-section">
            <form action="search.jsp" method="post" enctype="multipart/form-data">
                <input type="hidden" name="ACTION" value="UPLOAD">
                <p>File ID: <input type="text" name="fileId" placeholder="Can be empty"></p>
                <p>Select File: <input type="file" name="uploadFile"></p>
                <p><input type="submit" value="Upload File" style="background: #3498db; color: white; padding: 10px 20px; border: none; border-radius: 4px;"></p>
            </form>
        </div>
        
        <p><a href="search.jsp">Back to Search</a></p>
    </div>
</body>
</html>