// search.js - SecChamp Book Search Assets
// This file contains references to various JSP pages that can be discovered by attackers

// API Endpoints Configuration
const SECCHAMP_CONFIG = {
    version: "1.2.3",
    apiBaseUrl: "/tomcat-chal/api/",
    uploadBaseUrl: "/tomcat-chal/",
    
    // JSP Page References (Vulnerable: Exposes internal structure)
    pages: {
        search: "books/search.jsp",
        upload: "books/upload.jsp", 
        admin: "books/admin.jsp",
        fileManager: "books/search.jsp?ACTION=UPLOAD",
        deleteHandler: "books/search.jsp?ACTION=DELETE",
        updateHandler: "books/search.jsp?ACTION=UPDATE"
    },
    
    // API Endpoints
    apis: {
        // Legacy JSP APIs (vulnerable)
        secchamp: "../api/secchamp/index.jsp",
        general: "../api/general/index.jsp",
        evil: "../api/evil/index.jsp",  // Hidden but discoverable
        
        // Real Spring Boot API endpoints (exposed through JS)
        booksAPI: "http://localhost:8080/api/books",
        searchAPI: "http://localhost:8080/api/books/search", 
        categoriesAPI: "http://localhost:8080/api/books/categories",
        featuredAPI: "http://localhost:8080/api/books/featured",
        bookByIdAPI: "http://localhost:8080/api/books/{id}",
        categoryAPI: "http://localhost:8080/api/books/category/{category}"
    },
    
    // Security tokens (Vulnerable: Client-side token generation)
    tokens: {
        search: generateSearchToken(),
        upload: generateUploadToken(),
        admin: generateAdminToken()
    }
};

// Token generation functions (Vulnerable: Predictable patterns)
function generateSearchToken() {
    return "search_" + Math.random().toString(36).substr(2, 12) + "_" + Date.now();
}

function generateUploadToken() {
    return "upload_" + Math.random().toString(36).substr(2, 12) + "_" + Date.now();
}

function generateAdminToken() {
    return "admin_" + Math.random().toString(36).substr(2, 12) + "_" + Date.now();
}

// Real API Search functionality
async function performBookSearch(query, category, author) {
    console.log("Performing real API search with token:", SECCHAMP_CONFIG.tokens.search);
    
    try {
        // Build real API URL
        let searchUrl = SECCHAMP_CONFIG.apis.searchAPI + "?";
        const params = new URLSearchParams();
        
        if (query && query.trim()) params.append('title', query);
        if (author && author.trim()) params.append('author', author);
        if (category && category !== 'all') params.append('category', category);
        
        searchUrl += params.toString();
        console.log("Real API Search URL:", searchUrl);
        
        const response = await fetch(searchUrl);
        const data = await response.json();
        
        console.log("Real API Response:", data);
        
        if (data.status === "success") {
            return {
                status: "success",
                results: data.books || [],
                count: data.count || 0
            };
        } else {
            return {
                status: "error",
                message: data.message || "Search failed",
                results: []
            };
        }
    } catch (error) {
        console.error("Real API Search Error:", error);
        return {
            status: "error",
            message: "Failed to connect to Spring Boot API: " + error.message,
            results: []
        };
    }
}

// Additional API functions (Vulnerable: Direct API access)
async function getAllBooks() {
    try {
        const response = await fetch(SECCHAMP_CONFIG.apis.booksAPI);
        return await response.json();
    } catch (error) {
        console.error("Get all books error:", error);
        return { status: "error", message: error.message };
    }
}

async function getFeaturedBooks() {
    try {
        const response = await fetch(SECCHAMP_CONFIG.apis.featuredAPI);
        return await response.json();
    } catch (error) {
        console.error("Get featured books error:", error);
        return { status: "error", message: error.message };
    }
}

async function getCategories() {
    try {
        const response = await fetch(SECCHAMP_CONFIG.apis.categoriesAPI);
        return await response.json();
    } catch (error) {
        console.error("Get categories error:", error);
        return { status: "error", message: error.message };
    }
}

// File upload functionality (Vulnerable: No proper validation)
function uploadFile(fileData, fileId) {
    console.log("Uploading file with token:", SECCHAMP_CONFIG.tokens.upload);
    
    // Construct upload URL
    const uploadUrl = SECCHAMP_CONFIG.pages.upload;
    console.log("Upload URL:", uploadUrl);
    
    // Vulnerable: No file type validation
    // Vulnerable: Empty fileId is allowed
    if (!fileId || fileId.trim() === "") {
        console.warn("File ID is empty, but upload will proceed anyway");
    }
    
    return {
        status: "success",
        message: "File uploaded successfully",
        uploadUrl: uploadUrl
    };
}

// Admin functions (Should be hidden but exposed through JS)
function getAdminUrls() {
    return {
        adminPanel: SECCHAMP_CONFIG.pages.admin,
        deleteAll: SECCHAMP_CONFIG.pages.deleteHandler + "&fileName=*",
        fileManager: SECCHAMP_CONFIG.pages.fileManager
    };
}

// File manipulation functions (Vulnerable: Exposed file operations)
function deleteFile(fileName) {
    const deleteUrl = SECCHAMP_CONFIG.pages.deleteHandler + "&fileName=" + encodeURIComponent(fileName);
    console.log("Delete URL:", deleteUrl);
    
    // Vulnerable: Direct URL construction for file deletion
    return deleteUrl;
}

function updateFile(fileName, content) {
    const updateUrl = SECCHAMP_CONFIG.pages.updateHandler + "&fileName=" + encodeURIComponent(fileName) + "&content=" + encodeURIComponent(content);
    console.log("Update URL:", updateUrl);
    
    // Vulnerable: URL can be manipulated from UPDATE to DELETE
    return updateUrl;
}

// Initialize search functionality
function initSearch() {
    console.log("SecChamp Search initialized");
    console.log("Available pages:", SECCHAMP_CONFIG.pages);
    console.log("Available APIs:", SECCHAMP_CONFIG.apis);
    console.log("Generated tokens:", SECCHAMP_CONFIG.tokens);
    
    // Vulnerable: Expose configuration in global scope
    window.SECCHAMP_CONFIG = SECCHAMP_CONFIG;
}

// Auto-initialize when loaded
initSearch();

// Vulnerable: Expose all functions globally including real API access
window.SecChampSearch = {
    performBookSearch,
    getAllBooks,
    getFeaturedBooks,
    getCategories,
    uploadFile,
    getAdminUrls,
    deleteFile,
    updateFile,
    config: SECCHAMP_CONFIG
};

// Additional vulnerability: Direct API endpoint exposure
window.API_ENDPOINTS = SECCHAMP_CONFIG.apis;

console.log("search.js loaded - All functions exposed globally including real API access");
console.log("Try: window.SecChampSearch.getAdminUrls() to discover admin URLs");
console.log("Try: window.SecChampSearch.getAllBooks() to access real Spring Boot API");
console.log("Try: window.API_ENDPOINTS to see all exposed API endpoints");
console.log("Available real APIs:", Object.keys(SECCHAMP_CONFIG.apis).filter(key => key.includes('API')));