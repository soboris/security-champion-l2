<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="javax.servlet.http.Part" %>

<%
    // Generate a security token for this session
    String securityToken = UUID.randomUUID().toString();
    session.setAttribute("searchToken", securityToken);
    
    String action = request.getParameter("ACTION");
    String uploadPath = application.getRealPath("") + File.separator + "uploads";
    File uploadDir = new File(uploadPath);
    if (!uploadDir.exists()) uploadDir.mkdirs();
    
    String message = "";
    String errorMessage = "";
    
    // Handle file operations
    if (action != null) {
        if ("UPLOAD".equals(action)) {
            try {
                String fileName = "";
                String fileId = request.getParameter("fileId");
                
                // Get the uploaded file part
                Part filePart = request.getPart("uploadFile");
                if (filePart != null && filePart.getSize() > 0) {
                    // Get the filename
                    String contentDisposition = filePart.getHeader("content-disposition");
                    if (contentDisposition != null) {
                        for (String token : contentDisposition.split(";")) {
                            if (token.trim().startsWith("filename")) {
                                fileName = token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
                                break;
                            }
                        }
                    }
                    
                    if (fileName != null && !fileName.isEmpty()) {
                        // Server-side file type validation - Whitelist only
                        String fileExtension = "";
                        int lastDotIndex = fileName.lastIndexOf('.');
                        if (lastDotIndex > 0) {
                            fileExtension = fileName.substring(lastDotIndex + 1).toLowerCase();
                        }
                        
                        // Server-side file type validation - Whitelist only
                        String[] allowedExtensions = {"html", "txt", "png", "jpg", "gif"};
                        
                        boolean isAllowed = false;
                        for (String allowed : allowedExtensions) {
                            if (allowed.equals(fileExtension)) {
                                isAllowed = true;
                                break;
                            }
                        }
                        
                        if (!isAllowed) {
                            errorMessage = "File type '" + fileExtension.toUpperCase() + "' is not allowed. Only HTML, TXT, PNG, JPG, and GIF files are permitted.";
                        } else {
                            // File type is allowed, proceed with upload
                            File uploadedFile = new File(uploadPath, fileName);
                            
                            // Check if file already exists
                            if (uploadedFile.exists()) {
                                message += " (Warning: Overwriting existing file) ";
                            }
                            
                            // Process upload regardless of existing file
                            try (InputStream input = filePart.getInputStream();
                                 FileOutputStream output = new FileOutputStream(uploadedFile)) {
                                
                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = input.read(buffer)) != -1) {
                                    output.write(buffer, 0, bytesRead);
                                }
                            }
                            
                            message = "File uploaded successfully: " + fileName + " (Type: " + fileExtension.toUpperCase() + ")";
                        }
                        
                        // Process upload even with empty form fields
                        if (fileId == null || fileId.trim().isEmpty()) {
                            message += " (Warning: ID field was empty but upload proceeded)";
                        }
                    } else {
                        errorMessage = "No file selected or invalid filename";
                    }
                } else {
                    errorMessage = "No file uploaded";
                }
            } catch (Exception e) {
                errorMessage = "Upload failed: " + e.getMessage();
            }
        } else if ("DELETE".equals(action)) {
            // Allow file deletion through URL manipulation
            String fileName = request.getParameter("fileName");
            if (fileName != null) {
                File fileToDelete = new File(uploadPath, fileName);
                if (fileToDelete.exists() && fileToDelete.delete()) {
                    message = "File deleted successfully: " + fileName;
                } else {
                    errorMessage = "Failed to delete file: " + fileName;
                }
            }
        } else if ("UPDATE".equals(action)) {
            // Basic update functionality that can be manipulated
            String fileName = request.getParameter("fileName");
            String newContent = request.getParameter("content");
            if (fileName != null && newContent != null) {
                try {
                    File fileToUpdate = new File(uploadPath, fileName);
                    FileWriter writer = new FileWriter(fileToUpdate);
                    writer.write(newContent);
                    writer.close();
                    message = "File updated successfully: " + fileName;
                } catch (Exception e) {
                    errorMessage = "Update failed: " + e.getMessage();
                }
            }
        }
    }
%>

<!DOCTYPE html>
<html>
<head>
    <title>SecChamp Book Search</title>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
        .container { max-width: 800px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .header { background: #2c3e50; color: white; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
        .search-section { background: #ecf0f1; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
        .upload-section { background: #e8f5e8; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
        .message { background: #d4edda; color: #155724; padding: 10px; border-radius: 4px; margin: 10px 0; }
        .error { background: #f8d7da; color: #721c24; padding: 10px; border-radius: 4px; margin: 10px 0; }
        .token-info { background: #fff3cd; color: #856404; padding: 10px; border-radius: 4px; margin: 10px 0; font-size: 12px; }
        .btn { background: #3498db; color: white; padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; margin: 5px; }
        .btn:hover { background: #2980b9; }
        .danger-btn { background: #e74c3c; }
        .danger-btn:hover { background: #c0392b; }
        .popup { display: none; position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); 
                background: white; border: 2px solid #ccc; padding: 20px; border-radius: 8px; z-index: 1000;
                box-shadow: 0 4px 8px rgba(0,0,0,0.3); }
        .overlay { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; 
                  background: rgba(0,0,0,0.5); z-index: 999; }
        .file-list { background: #f8f9fa; padding: 10px; border-radius: 4px; margin: 10px 0; }
        .api-links { background: #e3f2fd; padding: 15px; border-radius: 5px; margin: 20px 0; }
        .api-links a { display: block; margin: 5px 0; color: #1976d2; text-decoration: none; }
        .api-links a:hover { text-decoration: underline; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>SecChamp Book Search Portal</h1>
            <p>Advanced book search and file management system</p>
        </div>

        <% if (!message.isEmpty()) { %>
            <div class="message"><%= message %></div>
        <% } %>
        
        <% if (!errorMessage.isEmpty()) { %>
            <div class="error"><%= errorMessage %></div>
        <% } %>



        <div class="search-section">
            <h3>Book Search</h3>
            <p>Click the button below to open the advanced search popup (now with real API integration):</p>
            <button class="btn" onclick="openSearchPopup()">Open Advanced Search</button>
            <button class="btn" onclick="loadSearchAssets()">Load Search Assets</button>
            <button class="btn" onclick="testRealAPI()">Test Real API</button>
        </div>

        <div class="upload-section">
            <h3>File Upload System</h3>
            <p><strong>Note:</strong> This system allows file uploads for book cover images and documents.</p>
            
            <form action="search.jsp?ACTION=UPLOAD" method="post" enctype="multipart/form-data">
                <input type="hidden" name="token" value="<%= securityToken %>">
                <table>
                    <tr>
                        <td>File ID:</td>
                        <td><input type="text" name="fileId" placeholder="Optional - can be left empty"></td>
                    </tr>
                    <tr>
                        <td>Select File:</td>
                        <td>
                            <input type="file" name="uploadFile" accept=".html,.txt,.png,.jpg,.gif">
                            <br><small style="color: #666; font-size: 11px;">
                                <strong>Allowed file types only:</strong> HTML, TXT, PNG, JPG, GIF<br>
                                All other file types will be rejected by server-side validation.
                            </small>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <button type="submit" class="btn">Upload File</button>
                        </td>
                    </tr>
                </table>
            </form>
        </div>

        <div class="file-list">
            <h4>Uploaded Files</h4>
            <%
                File[] files = uploadDir.listFiles();
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        String fileName = file.getName();
                        long fileSize = file.length();
                        String lastModified = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(file.lastModified()));
            %>
                <div style="border: 1px solid #ddd; padding: 15px; margin: 8px 0; border-radius: 4px; background: #fafafa;">
                    <div style="margin-bottom: 8px;">
                        <strong style="font-size: 14px; color: #2c3e50;"><%= fileName %></strong>
                    </div>
                    <div style="font-size: 12px; color: #666; margin-bottom: 10px;">
                        Size: <%= fileSize %> bytes | Modified: <%= lastModified %>
                    </div>
                    <div style="display: flex; gap: 8px;">
                        <a href="/uploads/<%= fileName %>" target="_blank" class="btn" style="font-size: 12px; padding: 6px 12px;">View File</a>                        
                    </div>
                </div>
            <%
                    }
                } else {
            %>
                <p><em>No files uploaded yet.</em></p>
            <% } %>
        </div>


    </div>

    <!-- Popup Overlay -->
    <div class="overlay" id="overlay" onclick="closeSearchPopup()"></div>

    <!-- Search Popup -->
    <div class="popup" id="searchPopup">
        <h3>Advanced Book Search</h3>
        <form id="searchForm">
            <p>Search Query: <input type="text" id="searchQuery" name="query" style="width: 200px;" placeholder="Enter book title..."></p>
            <p>Author: <input type="text" id="searchAuthor" name="author" style="width: 200px;" placeholder="Enter author name..."></p>
            <p>Category: 
                <select id="searchCategory" name="category">
                    <option value="">All Categories</option>
                    <option value="fiction">Fiction</option>
                    <option value="non-fiction">Non-Fiction</option>
                    <option value="mystery">Mystery</option>
                    <option value="romance">Romance</option>
                    <option value="science">Science</option>
                    <option value="technology">Technology</option>
                    <option value="history">History</option>
                    <option value="biography">Biography</option>
                </select>
            </p>
            <div id="searchResults" style="max-height: 300px; overflow-y: auto; margin: 10px 0; padding: 10px; background: #f8f9fa; border-radius: 4px; display: none;">
                <h4>Search Results</h4>
                <div id="resultsContent"></div>
            </div>
            <p>
                <button type="button" class="btn" onclick="performRealBookSearch()">Search Books</button>
                <button type="button" class="btn" onclick="closeSearchPopup()">Close</button>
            </p>
        </form>
    </div>

    <script>
        // Generate new tokens for different features
        function generateToken() {
            return 'token_' + Math.random().toString(36).substr(2, 16) + '_' + Date.now();
        }

        function openSearchPopup() {
            document.getElementById('overlay').style.display = 'block';
            document.getElementById('searchPopup').style.display = 'block';
            
            // Generate new token for popup
            console.log('New popup token generated:', generateToken());
            
            // Load available categories from API
            loadCategories();
        }

        // Real API search functionality
        async function performRealBookSearch() {
            // Generate new token for this search action
            const searchToken = generateToken();
            console.log('Perform Search - New token generated:', searchToken);
            
            const query = document.getElementById('searchQuery').value.trim();
            const author = document.getElementById('searchAuthor').value.trim();
            const category = document.getElementById('searchCategory').value;
            const resultsDiv = document.getElementById('searchResults');
            const resultsContent = document.getElementById('resultsContent');
            
            console.log('Performing real API search:', { query, author, category });
            
            try {
                // Build API URL
                let apiUrl = 'http://localhost:8080/api/books/search?';
                const params = new URLSearchParams();
                
                if (query) params.append('title', query);
                if (author) params.append('author', author);
                if (category) params.append('category', category);
                
                apiUrl += params.toString();
                console.log('API URL:', apiUrl);
                
                // Show loading
                resultsContent.innerHTML = '<p>Searching...</p>';
                resultsDiv.style.display = 'block';
                
                const response = await fetch(apiUrl);
                const data = await response.json();
                
                console.log('API Response:', data);
                
                if (data.status === 'success' && data.books) {
                    displaySearchResults(data.books, data.count);
                } else {
                    resultsContent.innerHTML = '<p>No books found or API error</p>';
                }
            } catch (error) {
                console.error('Search error:', error);
                resultsContent.innerHTML = '<p>Error connecting to API: ' + error.message + '</p>';
            }
        }
        
        function displaySearchResults(books, count) {
            const resultsContent = document.getElementById('resultsContent');
            
            if (books.length === 0) {
                resultsContent.innerHTML = '<p>No books found matching your criteria</p>';
                return;
            }
            
            let html = '<p><strong>Found ' + count + ' book(s):</strong></p>';
            html += '<div style="max-height: 200px; overflow-y: auto;">';
            
            books.forEach(book => {
                html += '<div style="border: 1px solid #ddd; padding: 8px; margin: 5px 0; border-radius: 4px; background: white;">';
                html += '<strong>' + escapeHtml(book.title) + '</strong><br>';
                html += '<em>by ' + escapeHtml(book.author) + '</em><br>';
                html += '<small>Category: ' + escapeHtml(book.category) + '</small><br>';
                if (book.price) {
                    html += '<small>Price: $' + book.price + '</small><br>';
                }
                if (book.description) {
                    html += '<p style="font-size: 11px; margin: 5px 0;">' + escapeHtml(book.description.substring(0, 100)) + '...</p>';
                }
                html += '</div>';
            });
            
            html += '</div>';
            resultsContent.innerHTML = html;
        }
        
        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }
        
        async function loadCategories() {
            // Generate new token for category loading action
            const categoryToken = generateToken();
            console.log('Load Categories - New token generated:', categoryToken);
            
            try {
                const response = await fetch('http://localhost:8080/api/books/categories');
                const data = await response.json();
                
                if (data.status === 'success' && data.categories) {
                    const select = document.getElementById('searchCategory');
                    // Keep the default "All Categories" option
                    const defaultOption = select.options[0];
                    select.innerHTML = '';
                    select.appendChild(defaultOption);
                    
                    // Add categories from API
                    data.categories.forEach(category => {
                        const option = document.createElement('option');
                        option.value = category.toLowerCase();
                        option.textContent = category.charAt(0).toUpperCase() + category.slice(1);
                        select.appendChild(option);
                    });
                    
                    console.log('Loaded categories from API:', data.categories);
                }
            } catch (error) {
                console.error('Error loading categories:', error);
            }
        }

        function closeSearchPopup() {
            document.getElementById('overlay').style.display = 'none';
            document.getElementById('searchPopup').style.display = 'none';
        }

        function loadSearchAssets() {
            // Generate new token for this action
            const actionToken = generateToken();
            console.log('Load Search Assets - New token generated:', actionToken);
            
            // Simulate loading a JS file that contains references to JSP pages and real API endpoints
            console.log('Loading search.js with JSP references and real API endpoints...');
            console.log('Discoverable JSP pages: upload.jsp, admin.jsp, search.jsp');
            console.log('Hidden API endpoints: /api/evil/index.jsp, /api/secchamp/class.jsp');
            
            // This would normally load books/search.js, but we'll simulate it
            const jsContent = `
                // search.js - Contains references to JSP pages and real Spring Boot API
                if (typeof API_ENDPOINTS === 'undefined') {
                    const API_ENDPOINTS = {
                        // Legacy JSP APIs (vulnerable - discoverable through JS)
                        secchamp: '../api/secchamp/index.jsp',
                        secchampClass: '../api/secchamp/class.jsp',
                        general: '../api/general/index.jsp',
                        evil: '../api/evil/index.jsp',
                        evilClass: '../api/evil/class.jsp',  // Hidden but discoverable
                        
                        // File management JSPs (vulnerable endpoints)
                        upload: 'upload.jsp',
                        admin: 'admin.jsp',
                        fileManager: 'search.jsp?ACTION=UPLOAD',
                        fileDelete: 'search.jsp?ACTION=DELETE&fileName=',
                        fileUpdate: 'search.jsp?ACTION=UPDATE&fileName=',
                        
                        // Real Spring Boot API endpoints (discoverable)
                        booksAPI: 'http://localhost:8080/api/books',
                        searchAPI: 'http://localhost:8080/api/books/search',
                        categoriesAPI: 'http://localhost:8080/api/books/categories',
                        featuredAPI: 'http://localhost:8080/api/books/featured',
                    };
                    
                    // Expose all API endpoints including dangerous ones
                    window.EXPOSED_APIS = API_ENDPOINTS;
                }
                
                // Avoid redeclaring functions if already loaded
                if (typeof window.callUploadAPI === 'undefined') {
                    function callUploadAPI() {
                        window.location.href = 'search.jsp?ACTION=UPLOAD';
                    }
                    
                    function callDeleteAPI(fileName) {
                        window.location.href = 'search.jsp?ACTION=DELETE&fileName=' + fileName;
                    }
                    
                    function callUpdateAPI(fileName) {
                        window.location.href = 'search.jsp?ACTION=UPDATE&fileName=' + fileName;
                    }
                    
                    // Expose direct API access functions
                    function getAllBooks() {
                        const token = 'api_' + Math.random().toString(36).substr(2, 12) + '_' + Date.now();
                        console.log('getAllBooks - Token generated:', token);
                        return fetch(window.EXPOSED_APIS.booksAPI).then(r => r.json());
                    }
                    
                    function searchBooks(params) {
                        const token = 'search_' + Math.random().toString(36).substr(2, 12) + '_' + Date.now();
                        console.log('searchBooks - Token generated:', token);
                        const url = window.EXPOSED_APIS.searchAPI + '?' + new URLSearchParams(params);
                        return fetch(url).then(r => r.json());
                    }
                    
                    function getFeaturedBooks() {
                        const token = 'featured_' + Math.random().toString(36).substr(2, 12) + '_' + Date.now();
                        console.log('getFeaturedBooks - Token generated:', token);
                        return fetch(window.EXPOSED_APIS.featuredAPI).then(r => r.json());
                    }
                    
                    // Make functions globally accessible (vulnerability)
                    window.BookAPI = { getAllBooks, searchBooks, getFeaturedBooks };
                    window.callUploadAPI = callUploadAPI;
                    window.callDeleteAPI = callDeleteAPI;
                    window.callUpdateAPI = callUpdateAPI;
                }
            `;
            
            // Create and inject the script
            const script = document.createElement('script');
            script.textContent = jsContent;
            document.head.appendChild(script);
            
            alert('Search assets loaded! Real API endpoints exposed. Check browser console for details.');
            console.log('Loaded search.js content:', jsContent);
            console.log('Try: window.EXPOSED_APIS to see all endpoints');
            console.log('Try: window.BookAPI.getAllBooks() to access the API directly');
        }

        // Test real API functionality
        async function testRealAPI() {
            // Generate new token for this action
            const actionToken = generateToken();
            console.log('Test Real API - New token generated:', actionToken);
            
            console.log('Testing real Spring Boot API...');
            
            try {
                // Test basic API connectivity
                const response = await fetch('http://localhost:8080/api/books');
                const data = await response.json();
                
                if (data.success) {
                    alert('API Connection Successful!\n\nFound ' + data.count + ' books in the database.\n\nCheck browser console for full API response.');
                    console.log('API Test Response:', data);
                    
                    // Show some sample data
                    if (data.data && data.data.length > 0) {
                        console.log('Sample book:', data.data[0]);
                    }
                } else {
                    alert('API responded but with error:\n' + data.message);
                }
            } catch (error) {
                alert('API Connection Failed!\n\nError: ' + error.message + '\n\nMake sure Spring Boot application is running on port 8080.');
                console.error('API Test Error:', error);
            }
        }
       
    </script>
</body>
</html>