### `PurchaseApiController.java` 
is the original controller that is vulnerable to race condition (`sourcecodes/springboot-chal/src/main/java/com/cybersecurity/sechamp2025/controllers/api/PurchaseApiController.java`)

### `PurchaseApiController.java` 
in this directory is the fixed version, you can copy the file content to replace `sourcecodes/springboot-chal/src/main/java/com/cybersecurity/sechamp2025/controllers/api/PurchaseApiController.java`

### To test
`python race-test.py` to test for race condition

# Race Condition Code Fix Solutions

## Overview

This document provides a comprehensive analysis of race condition vulnerabilities in concurrent stock management systems and demonstrates how to fix them using Java's atomic operations with the `compareAndSet` method.

Fix .java location: `racecondition/PurchaseApiController_Fixed.java`

## Table of Contents
- [Problem Statement](#problem-statement)
- [Vulnerability Analysis](#vulnerability-analysis)
- [Solution Implementation](#solution-implementation)
- [Code Comparison](#code-comparison)
- [Testing Results](#testing-results)
- [Best Practices](#best-practices)

## Problem Statement

### Race Condition Vulnerability
In multi-threaded environments, race conditions occur when multiple threads access shared resources simultaneously without proper synchronization. In our e-commerce application, this manifests as:

- **Overselling**: Stock goes negative when multiple users purchase the last item simultaneously
- **Data Inconsistency**: Database stock doesn't match actual inventory
- **Financial Loss**: Selling more items than available

### Vulnerable Code Pattern
```java
// VULNERABLE: Non-atomic read-then-modify-then-write operation
Book book = bookService.findById(bookId);
int currentStock = book.getStock();  // Thread A reads: 1, Thread B reads: 1
book.setStock(currentStock - 1);     // Thread A sets: 0, Thread B sets: 0
bookService.save(book);              // Result: Both succeed, stock = 0 (should be -1)
```

## Vulnerability Analysis

### Original Vulnerable Implementation
**Location**: `PurchaseApiController.java` - `confirmPurchase` method

```java
// Lines 201-205: VULNERABLE CODE
Book currentBook = bookService.findById(session.getBookId());
int currentStock = currentBook.getStock();

// RACE CONDITION GAP: Other threads can modify stock here
currentBook.setStock(currentStock - 1);  // Allows negative stock!
bookService.save(currentBook);
```

### Race Condition Scenario
1. **Initial State**: Book stock = 1
2. **Thread A**: Reads stock = 1
3. **Thread B**: Reads stock = 1 (same value!)
4. **Thread A**: Sets stock = 0, saves to database
5. **Thread B**: Sets stock = 0, saves to database
6. **Result**: Both purchases succeed, but stock should be -1

## Solution Implementation

### Fixed Implementation using AtomicInteger.compareAndSet()

**Location**: `racecondition/PurchaseApiController_Fixed.java`

#### 1. Thread-Safe Stock Storage (Line 45)
```java
private final ConcurrentHashMap<String, AtomicInteger> atomicStockMap = new ConcurrentHashMap<>();
```

#### 2. Atomic Stock Initialization (Lines 304-311)
```java
private AtomicInteger getOrCreateAtomicStock(String bookId) {
    return atomicStockMap.computeIfAbsent(bookId, id -> {
        Book book = bookService.findById(id);
        int initialStock = (book != null && book.getStock() != null) ? book.getStock() : 0;
        return new AtomicInteger(initialStock);
    });
}
```

#### 3. Core Race Condition Fix (Lines 319-365)
```java
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
```

#### 4. Atomic Stock Check Method (Lines 313-317)
```java
private boolean atomicStockCheck(Book book) {
    AtomicInteger atomicStock = getOrCreateAtomicStock(book.getId());
    return atomicStock.get() > 0;
}
```

#### 5. Database Synchronization Method (Lines 353-359)
```java
private void syncAtomicStockWithDatabase(String bookId) {
    Book book = bookService.findById(bookId);
    if (book != null && book.getStock() != null) {
        AtomicInteger atomicStock = getOrCreateAtomicStock(bookId);
        atomicStock.set(book.getStock());
    }
}
```

#### 6. Integration Points
```java
// Line 95 (initiatePurchase):
if (!atomicStockCheck(book)) {
    return ResponseEntity.status(400).body(Map.of(
        "error", "Out of stock", 
        "message", "This book is currently out of stock"
    ));
}

// Line 189 (confirmPurchase):
boolean stockUpdateSuccess = atomicStockDecrement(book);

if (!stockUpdateSuccess) {
    purchaseSessionService.cancelSession(sessionId);
    return ResponseEntity.status(400).body(Map.of(
        "error", "Purchase failed", 
        "message", "Book went out of stock during purchase confirmation"
    ));
}
```

## Code Comparison

### Before (Vulnerable)
```java
// ❌ VULNERABLE: Race condition possible
Book book = bookService.findById(bookId);
int stock = book.getStock();
book.setStock(stock - 1);  // Multiple threads can execute this
bookService.save(book);
```

### After (Secure)
```java
// ✅ SECURE: Atomic operation with retry
AtomicInteger atomicStock = getOrCreateAtomicStock(bookId);
int current = atomicStock.get();
int newValue = current - 1;

// Only ONE thread's compareAndSet will succeed
if (atomicStock.compareAndSet(current, newValue)) {
    // Update database
    book.setStock(newValue);
    bookService.save(book);
    return true;
} else {
    // Another thread modified the value, retry
}
```

## How compareAndSet Works

### Atomic Compare-and-Set Pattern
```java
AtomicInteger stock = new AtomicInteger(5);

// Thread 1 and Thread 2 both try to decrement simultaneously
int current = stock.get();        // Both read: 5
int newValue = current - 1;       // Both calculate: 4

// Only ONE thread's compareAndSet will succeed:
if (stock.compareAndSet(5, 4)) {  // Thread 1: SUCCESS (stock becomes 4)
    return true;
} else {                          // Thread 2: FAILURE (stock was changed by Thread 1)
    // Retry with updated value
}
```

### Key Benefits
1. **Lock-Free**: No thread blocking or waiting
2. **High Performance**: Uses CPU-level atomic instructions  
3. **Automatic Retry**: Failed operations can retry immediately
4. **Guaranteed Consistency**: Either succeeds atomically or fails cleanly

## Testing Results

### Vulnerability Testing Script
**File**: `racecondition/race-test.py`

## Best Practices

### 1. Use Atomic Operations
```java
✅ AtomicInteger.compareAndSet(expected, update)
✅ AtomicReference.compareAndSet(expected, update)
❌ Non-atomic read-modify-write operations
```

### 2. Implement Retry Logic
```java
for (int attempt = 0; attempt < maxRetries; attempt++) {
    if (atomicValue.compareAndSet(current, newValue)) {
        return true; // Success
    }
    // Automatic retry on failure
}
```

### 3. Handle Database Consistency
```java
if (atomicStock.compareAndSet(current, newValue)) {
    try {
        updateDatabase(newValue);
    } catch (Exception e) {
        // Rollback atomic operation
        atomicStock.compareAndSet(newValue, current);
        throw e;
    }
}
```

### 4. Performance Considerations
- Use `ConcurrentHashMap` for thread-safe collections
- Prefer `AtomicInteger` over `volatile int` for counters
- Set appropriate retry limits to prevent infinite loops
- Consider using `AtomicStampedReference` for ABA problem prevention

## Security Impact

### Before Fix
- ❌ **Race Condition**: Multiple threads can oversell inventory
- ❌ **Data Corruption**: Inconsistent stock levels
- ❌ **Financial Loss**: Negative inventory = lost revenue
- ❌ **User Experience**: Disappointed customers

### After Fix
- ✅ **Thread Safety**: Atomic operations prevent race conditions
- ✅ **Data Integrity**: Stock levels remain consistent
- ✅ **Financial Protection**: No overselling possible
- ✅ **Reliable Service**: Predictable behavior under load

## Conclusion

The race condition vulnerability in stock management was successfully fixed using Java's `AtomicInteger.compareAndSet()` method with a retry loop. This solution provides:

1. **Lock-free concurrency** using atomic operations
2. **Automatic retry mechanism** for handling concurrent modifications
3. **Database synchronization** with rollback capability
4. **High performance** under concurrent load

The fix ensures that stock updates are atomic and thread-safe, preventing overselling even under high concurrent load while maintaining optimal performance.

## Additional Resources

- [Java Atomic Operations Documentation](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/package-summary.html)
- [Compare-and-Set Algorithm](https://en.wikipedia.org/wiki/Compare-and-swap)
- [Lock-Free Programming](https://preshing.com/20120612/an-introduction-to-lock-free-programming/)




