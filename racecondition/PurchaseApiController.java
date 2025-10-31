package com.cybersecurity.sechamp2025.controllers.api;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cybersecurity.sechamp2025.models.Book;
import com.cybersecurity.sechamp2025.models.PurchaseSession;
import com.cybersecurity.sechamp2025.models.User;
import com.cybersecurity.sechamp2025.services.BookService;
import com.cybersecurity.sechamp2025.services.PurchaseSessionService;
import com.cybersecurity.sechamp2025.services.UserBookService;
import com.cybersecurity.sechamp2025.services.UserService;
import com.cybersecurity.sechamp2025.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseApiController{

    @Autowired
    private UserService userService;
    @Autowired
    private BookService bookService;

    @Autowired
    private PurchaseSessionService purchaseSessionService;

    @Autowired
    private UserBookService userBookService;

    // SECURE: Atomic stock tracking using ConcurrentHashMap of AtomicIntegers
    private final ConcurrentHashMap<String, AtomicInteger> atomicStockMap = new ConcurrentHashMap<>();

    @PostMapping("/book")
    public ResponseEntity<?> initiatePurchase(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        // Get user from token
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        String token = authHeader.substring(7);
        String userId = JwtUtil.validateAndExtractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        // Check if user status is PENDING
        if ("PENDING".equals(user.getAccountStatus())) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Purchase not allowed", 
                "message", "Your account is pending approval. Please contact an administrator to activate your account before making purchases."
            ));
        }

        // Check if user account is not ACTIVE
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Purchase not allowed", 
                "message", "Your account status does not allow purchases. Current status: " + user.getAccountStatus()
            ));
        }

        // Get book details
        Object bookIdObj = request.get("bookId");
        if (bookIdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Book ID is required"));
        }
        String bookId = String.valueOf(bookIdObj);

        Book book = bookService.findById(bookId);
        if (book == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Book not found"));
        }

        // SECURE: Atomic stock check using compareAndSet
        if (!atomicStockCheck(book)) {
            return ResponseEntity.status(400).body(Map.of(
                "error", "Out of stock", 
                "message", "This book is currently out of stock"
            ));
        }

        // Check if user has sufficient credit limit
        BigDecimal bookPrice = book.getPrice();
        if (bookPrice != null && user.getCreditLimit().compareTo(bookPrice) < 0) {
            return ResponseEntity.status(400).body(Map.of(
                "error", "Insufficient credit limit", 
                "message", "Your credit limit ($" + user.getCreditLimit() + ") is insufficient for this purchase ($" + bookPrice + ")"
            ));
        }

        // Create purchase session to temporarily lock the book
        PurchaseSession session = purchaseSessionService.createSession(userId, bookId);

        // Return session info for confirmation page
        return ResponseEntity.ok(Map.of(
            "sessionId", session.getId(),
            "message", "Book temporarily reserved",
            "bookTitle", book.getTitle(),
            "bookAuthor", book.getAuthor(),
            "price", bookPrice,
            "expiresAt", session.getExpiresAt().toEpochMilli(),
            "remainingCredit", user.getCreditLimit().subtract(bookPrice != null ? bookPrice : BigDecimal.ZERO)
        ));
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPurchase(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        // Get user from token
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        String token = authHeader.substring(7);
        String userId = JwtUtil.validateAndExtractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        // Get session ID
        Object sessionIdObj = request.get("sessionId");
        if (sessionIdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Session ID is required"));
        }
        String sessionId = String.valueOf(sessionIdObj);

        // Get purchase session
        Optional<PurchaseSession> sessionOpt = purchaseSessionService.getSessionById(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Purchase session not found or expired"));
        }

        PurchaseSession session = sessionOpt.get();
        
        // Verify session belongs to user
        if (!session.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Session does not belong to user"));
        }

        // Check if session is expired
        if (session.isExpired()) {
            purchaseSessionService.cancelSession(sessionId);
            return ResponseEntity.status(400).body(Map.of("error", "Purchase session has expired"));
        }

        // Get book details
        Book book = bookService.findById(session.getBookId());
        if (book == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Book not found"));
        }

        // Check credit limit again
        BigDecimal bookPrice = book.getPrice();
        if (bookPrice != null && user.getCreditLimit().compareTo(bookPrice) < 0) {
            purchaseSessionService.cancelSession(sessionId);
            return ResponseEntity.status(400).body(Map.of(
                "error", "Insufficient credit limit", 
                "message", "Your credit limit is insufficient for this purchase"
            ));
        }

        try {
            // SECURE: Use atomic compareAndSet to prevent race conditions
            boolean stockUpdateSuccess = atomicStockDecrement(book);
            
            if (!stockUpdateSuccess) {
                purchaseSessionService.cancelSession(sessionId);
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Purchase failed", 
                    "message", "Book went out of stock during purchase confirmation"
                ));
            }

            // Update user's credit limit
            BigDecimal newCreditLimit = user.getCreditLimit().subtract(bookPrice != null ? bookPrice : BigDecimal.ZERO);
            user.setCreditLimit(newCreditLimit);
            userService.updateUser(user);

            // Add book to user's library (allow multiple copies)
            userBookService.addBookCopyToUser(userId, session.getBookId(), bookPrice);

            // Complete the session
            purchaseSessionService.completeSession(sessionId);

            // Get updated book for final stock count
            Book updatedBook = bookService.findById(session.getBookId());

            return ResponseEntity.ok(Map.of(
                "message", "Purchase completed successfully",
                "bookTitle", updatedBook.getTitle(),
                "price", bookPrice,
                "remainingCredit", newCreditLimit,
                "remainingStock", updatedBook.getStock()
            ));

        } catch (Exception e) {
            // If anything fails, cancel the session
            purchaseSessionService.cancelSession(sessionId);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Purchase failed", 
                "message", "An error occurred while processing your purchase: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelPurchase(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        // Get user from token
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        String token = authHeader.substring(7);
        String userId = JwtUtil.validateAndExtractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        // Get session ID
        Object sessionIdObj = request.get("sessionId");
        if (sessionIdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Session ID is required"));
        }
        String sessionId = String.valueOf(sessionIdObj);

        // Get purchase session
        Optional<PurchaseSession> sessionOpt = purchaseSessionService.getSessionById(sessionId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Session already expired or cancelled"));
        }

        PurchaseSession session = sessionOpt.get();
        
        // Verify session belongs to user
        if (!session.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Session does not belong to user"));
        }

        // Cancel the session
        purchaseSessionService.cancelSession(sessionId);

        return ResponseEntity.ok(Map.of("message", "Purchase cancelled successfully"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getPurchaseStatus(HttpServletRequest httpRequest) {
        // Get user from token
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        String token = authHeader.substring(7);
        String userId = JwtUtil.validateAndExtractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        boolean canPurchase = "ACTIVE".equals(user.getAccountStatus());
        
        return ResponseEntity.ok(Map.of(
            "canPurchase", canPurchase,
            "accountStatus", user.getAccountStatus(),
            "creditLimit", user.getCreditLimit(),
            "message", canPurchase ? "Account is active and ready for purchases" : 
                      "PENDING".equals(user.getAccountStatus()) ? 
                      "Account is pending approval. Contact admin to activate." :
                      "Account status does not allow purchases: " + user.getAccountStatus()
        ));
    }

    // SECURE: Initialize atomic stock tracking for a book
    private AtomicInteger getOrCreateAtomicStock(String bookId) {
        return atomicStockMap.computeIfAbsent(bookId, id -> {
            Book book = bookService.findById(id);
            int initialStock = (book != null && book.getStock() != null) ? book.getStock() : 0;
            return new AtomicInteger(initialStock);
        });
    }

    // SECURE: Atomic stock check method using AtomicInteger
    private boolean atomicStockCheck(Book book) {
        AtomicInteger atomicStock = getOrCreateAtomicStock(book.getId());
        return atomicStock.get() > 0;
    }

    // SECURE: Atomic stock decrement using compareAndSet with retry loop
    private boolean atomicStockDecrement(Book book) {
        AtomicInteger atomicStock = getOrCreateAtomicStock(book.getId());
        int maxRetries = 1000;  // High number of retries for compareAndSet
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            int currentStock = atomicStock.get();
            
            // Check if stock is available
            if (currentStock <= 0) {
                return false; // Out of stock
            }
            
            int newStock = currentStock - 1;
            
            // ATOMIC COMPARE-AND-SET: The core race condition prevention
            if (atomicStock.compareAndSet(currentStock, newStock)) {
                // Successfully decremented atomically!
                // Now update the database to reflect the change
                try {
                    Book dbBook = bookService.findById(book.getId());
                    if (dbBook != null) {
                        dbBook.setStock(newStock);
                        bookService.save(dbBook);
                    }
                    return true; // Success
                } catch (Exception e) {
                    // If database update fails, rollback the atomic counter
                    atomicStock.compareAndSet(newStock, currentStock);
                    return false;
                }
            }
            // compareAndSet failed, another thread modified the value
            // The for loop will automatically retry
        }
        
        return false; // Failed after max retries
    }

    // SECURE: Sync atomic stock with database (useful for initialization)
    private void syncAtomicStockWithDatabase(String bookId) {
        Book book = bookService.findById(bookId);
        if (book != null && book.getStock() != null) {
            AtomicInteger atomicStock = getOrCreateAtomicStock(bookId);
            atomicStock.set(book.getStock());
        }
    }

    // SECURE: Race condition test endpoint using AtomicInteger.compareAndSet()
    @PostMapping("/race-test-secure/{bookId}")
    public ResponseEntity<?> raceConditionTestSecure(@PathVariable String bookId, HttpServletRequest httpRequest) {
        // Get user from token
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        String token = authHeader.substring(7);
        String userId = JwtUtil.validateAndExtractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        try {
            Book book = bookService.findById(bookId);
            if (book == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Book not found"));
            }
            
            // Ensure atomic stock is synced with database
            syncAtomicStockWithDatabase(bookId);
            
            // SECURE: Use AtomicInteger.compareAndSet() with retry loop
            boolean success = atomicStockDecrement(book);
            
            // Get updated stock from atomic counter
            AtomicInteger atomicStock = getOrCreateAtomicStock(bookId);
            int finalStock = atomicStock.get();
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "bookId", bookId,
                "title", book.getTitle(),
                "currentStock", finalStock,
                "message", success ? "Secure atomic compareAndSet completed" : "Purchase failed - out of stock",
                "securityNote", "Uses AtomicInteger.compareAndSet() with retry loop to prevent race conditions",
                "implementation", "Java's native atomic operations with lock-free programming"
            ));
            
        } catch (Exception e) {
            Book book = bookService.findById(bookId);
            AtomicInteger atomicStock = getOrCreateAtomicStock(bookId);
            int stock = atomicStock.get();
            
            return ResponseEntity.ok(Map.of(
                "success", false,
                "bookId", bookId,
                "title", book != null ? book.getTitle() : "Unknown",
                "currentStock", stock,
                "message", "Secure race condition test error: " + e.getMessage(),
                "implementation", "AtomicInteger.compareAndSet() with error handling"
            ));
        }
    }
}
