<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Hello World JSP</title>
</head>
<body>
    <h1>Hello World from JSP!</h1>

    <%
        // Simple Java code inside JSP
        String name = "Visitor";
        out.println("<p>Welcome, " + name + "!</p>");
    %>

    <p>Current time: <%= new java.util.Date() %></p>
</body>
</html>