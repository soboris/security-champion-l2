<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true"%>
<!DOCTYPE html>
<html>
<head>
    <title>Error - SecChamp</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; background-color: #f5f5f5; }
        .error-container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .error-header { background: #e74c3c; color: white; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
        .error-details { background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 10px 0; }
        .back-link { margin-top: 20px; }
        .back-link a { color: #3498db; text-decoration: none; }
        .back-link a:hover { text-decoration: underline; }
    </style>
</head>
<body>
    <div class="error-container">
        <div class="error-header">
            <h1>Application Error</h1>
        </div>
        
        <p>An error occurred while processing your request.</p>
        
        <div class="error-details">
            <strong>Error Code:</strong> <%= response.getStatus() %><br>
            <strong>Request URI:</strong> <%= request.getRequestURI() %><br>
            <strong>Time:</strong> <%= new java.util.Date() %>
        </div>
        
        <% if (exception != null) { %>
        <div class="error-details">
            <strong>Error Message:</strong> <%= exception.getMessage() %>
        </div>
        <% } %>
        
        <div class="back-link">
            <a href="javascript:history.back()">Go Back</a> | 
            <a href="/">Home</a> |
            <a href="/books/search.jsp">Search Books</a>
        </div>
    </div>
</body>
</html>