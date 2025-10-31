package com.cybersecurity.sechamp2025.controllers.api;

import com.cybersecurity.sechamp2025.models.Book;
import com.cybersecurity.sechamp2025.models.BookReview;
import com.cybersecurity.sechamp2025.services.BookService;
import com.cybersecurity.sechamp2025.services.BookReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = {"http://localhost:18080", "http://localhost:8080"}, allowCredentials = "true")
public class BookApiController {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookReviewService bookReviewService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllBooks() {
        try {
            Map<String, Object> response = new HashMap<>();
            List<Book> books = bookService.findAll();
            
            response.put("success", true);
            response.put("data", books);
            response.put("count", books.size());
            response.put("message", "Books retrieved successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving books: " + e.getMessage());
            errorResponse.put("data", null);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBookById(@PathVariable String id) {
        try {
            Map<String, Object> response = new HashMap<>();
            Book book = bookService.findById(id);
            
            if (book == null) {
                response.put("success", false);
                response.put("message", "Book not found with ID: " + id);
                response.put("data", null);
                return ResponseEntity.notFound().build();
            }
            
            response.put("success", true);
            response.put("data", book);
            response.put("message", "Book found successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving book: " + e.getMessage());
            errorResponse.put("data", null);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getBooksByCategory(@PathVariable String category) {
        Map<String, Object> response = new HashMap<>();
        List<Book> books = bookService.findByCategory(category);
        
        response.put("status", "success");
        response.put("category", category);
        response.put("count", books.size());
        response.put("books", books);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category) {
        
        Map<String, Object> response = new HashMap<>();
        List<Book> books;
        
        if (title != null && !title.trim().isEmpty()) {
            books = bookService.searchByTitle(title);
        } else if (author != null && !author.trim().isEmpty()) {
            books = bookService.findByAuthor(author);
        } else if (category != null && !category.trim().isEmpty()) {
            books = bookService.findByCategory(category);
        } else {
            books = bookService.findAll();
        }
        
        response.put("status", "success");
        response.put("count", books.size());
        response.put("books", books);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategories() {
        Map<String, Object> response = new HashMap<>();
        List<String> categories = bookService.getCategories();
        
        response.put("status", "success");
        response.put("categories", categories);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/featured")
    public ResponseEntity<Map<String, Object>> getFeaturedBooks() {
        Map<String, Object> response = new HashMap<>();
        List<Book> books = bookService.getFeaturedBooks();
        
        response.put("status", "success");
        response.put("count", books.size());
        response.put("books", books);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createBook(@RequestBody Book book) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Book savedBook = bookService.save(book);
            response.put("status", "success");
            response.put("message", "Book created successfully");
            response.put("book", savedBook);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to create book: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateBook(@PathVariable String id, @RequestBody Book book) {
        Map<String, Object> response = new HashMap<>();
        
        Book existingBook = bookService.findById(id);
        if (existingBook == null) {
            response.put("status", "error");
            response.put("message", "Book not found");
            return ResponseEntity.notFound().build();
        }
        
        book.setId(id);
        Book updatedBook = bookService.save(book);
        
        response.put("status", "success");
        response.put("message", "Book updated successfully");
        response.put("book", updatedBook);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBook(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        
        Book existingBook = bookService.findById(id);
        if (existingBook == null) {
            response.put("status", "error");
            response.put("message", "Book not found");
            return ResponseEntity.notFound().build();
        }
        
        bookService.deleteById(id);
        
        response.put("status", "success");
        response.put("message", "Book deleted successfully");
        
        return ResponseEntity.ok(response);
    }

    // ==================== BOOK REVIEW ENDPOINTS ====================

    /**
     * Get reviews for a specific book
     */
    @GetMapping("/{bookId}/reviews")
    public ResponseEntity<Map<String, Object>> getBookReviews(@PathVariable String bookId) {
        try {
            List<BookReview> reviews = bookReviewService.findByBookId(bookId);
            BigDecimal averageRating = bookReviewService.getAverageRatingForBook(bookId);
            Long reviewCount = bookReviewService.getReviewCountForBook(bookId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviews);
            response.put("count", reviewCount);
            response.put("averageRating", averageRating);
            response.put("bookId", bookId);
            response.put("message", "Reviews retrieved successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving reviews: " + e.getMessage());
            errorResponse.put("data", null);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Create a new book review
     */
    @PostMapping("/{bookId}/reviews")
    public ResponseEntity<Map<String, Object>> createBookReview(
            @PathVariable String bookId,
            @RequestBody BookReview review) {
        try {
            review.setBookId(bookId);
            
            if (review.getUserId() == null || review.getRating() == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "User ID and rating are required");
                errorResponse.put("data", null);
                
                return ResponseEntity.badRequest().body(errorResponse);
            }

            BookReview savedReview = bookReviewService.save(review);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", savedReview);
            response.put("message", "Review created successfully");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("data", null);
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error creating review: " + e.getMessage());
            errorResponse.put("data", null);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get all book reviews
     */
    @GetMapping("/reviews")
    public ResponseEntity<Map<String, Object>> getAllReviews() {
        try {
            List<BookReview> reviews = bookReviewService.findAll();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviews);
            response.put("count", reviews.size());
            response.put("message", "All reviews retrieved successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving reviews: " + e.getMessage());
            errorResponse.put("data", null);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Search reviews by keyword
     */
    @GetMapping("/reviews/search")
    public ResponseEntity<Map<String, Object>> searchReviews(@RequestParam String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Keyword parameter is required and cannot be empty");
                errorResponse.put("data", null);
                
                return ResponseEntity.badRequest().body(errorResponse);
            }

            List<BookReview> reviews = bookReviewService.searchByKeyword(keyword.trim());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviews);
            response.put("count", reviews.size());
            response.put("searchTerm", keyword.trim());
            response.put("message", "Review search completed successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error searching reviews: " + e.getMessage());
            errorResponse.put("data", null);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get reviews by rating
     */
    @GetMapping("/reviews/rating/{minRating}")
    public ResponseEntity<Map<String, Object>> getReviewsByRating(@PathVariable BigDecimal minRating) {
        try {
            List<BookReview> reviews = bookReviewService.findByRatingGreaterThanEqual(minRating);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviews);
            response.put("count", reviews.size());
            response.put("minRating", minRating);
            response.put("message", "Reviews filtered by rating successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error filtering reviews by rating: " + e.getMessage());
            errorResponse.put("data", null);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get verified purchase reviews only
     */
    @GetMapping("/reviews/verified")
    public ResponseEntity<Map<String, Object>> getVerifiedReviews() {
        try {
            List<BookReview> reviews = bookReviewService.findVerifiedPurchaseReviews();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviews);
            response.put("count", reviews.size());
            response.put("message", "Verified purchase reviews retrieved successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving verified reviews: " + e.getMessage());
            errorResponse.put("data", null);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get reviews by user ID
     */
    @GetMapping("/reviews/user/{userId}")
    public ResponseEntity<Map<String, Object>> getReviewsByUser(@PathVariable String userId) {
        try {
            List<BookReview> reviews = bookReviewService.findByUserId(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviews);
            response.put("count", reviews.size());
            response.put("userId", userId);
            response.put("message", "User reviews retrieved successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving user reviews: " + e.getMessage());
            errorResponse.put("data", null);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Mark a review as helpful
     */
    @PostMapping("/reviews/{reviewId}/helpful")
    public ResponseEntity<Map<String, Object>> markReviewHelpful(@PathVariable String reviewId) {
        try {
            BookReview updatedReview = bookReviewService.incrementHelpfulCount(reviewId);
            Map<String, Object> response = new HashMap<>();
            
            if (updatedReview != null) {
                response.put("success", true);
                response.put("data", updatedReview);
                response.put("message", "Review marked as helpful");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Review not found with ID: " + reviewId);
                response.put("data", null);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error updating review: " + e.getMessage());
            errorResponse.put("data", null);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Handle CORS preflight requests
    @RequestMapping(method = RequestMethod.OPTIONS, value = "/**")
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With")
                .header("Access-Control-Max-Age", "3600")
                .build();
    }
}
