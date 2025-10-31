package com.cybersecurity.sechamp2025.services;

import com.cybersecurity.sechamp2025.models.ShoppingCart;
import com.cybersecurity.sechamp2025.models.CartItem;
import com.cybersecurity.sechamp2025.models.Book;
import com.cybersecurity.sechamp2025.repositories.ShoppingCartRepository;
import com.cybersecurity.sechamp2025.repositories.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShoppingCartService {

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private BookService bookService;


    

    public ShoppingCart getOrCreateActiveCart(String userId) {
        Optional<ShoppingCart> existingCart = shoppingCartRepository.findActiveCartByUserId(userId);
        
        if (existingCart.isPresent()) {
            ShoppingCart cart = existingCart.get();
            if (!cart.isExpired()) {
                return cart;
            } else {
                // Expire the cart and create a new one
                cart.setStatus("EXPIRED");
                shoppingCartRepository.save(cart);
            }
        }
        
        // Create new cart
        String cartId = UUID.randomUUID().toString();
        ShoppingCart newCart = new ShoppingCart(cartId, userId);
        return shoppingCartRepository.save(newCart);
    }

    public Optional<ShoppingCart> getActiveCart(String userId) {
        Optional<ShoppingCart> cart = shoppingCartRepository.findActiveCartByUserId(userId);
        if (cart.isPresent() && cart.get().isExpired()) {
            cart.get().setStatus("EXPIRED");
            shoppingCartRepository.save(cart.get());
            return Optional.empty();
        }
        return cart;
    }

    @Transactional
    public CartItem addBookToCart(String userId, String bookId) {
        return addBookToCart(userId, bookId, 1);
    }

    @Transactional
    public CartItem addBookToCart(String userId, String bookId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }
        
        // Get or create active cart
        ShoppingCart cart = getOrCreateActiveCart(userId);
        
        // Check if book already in cart
        Optional<CartItem> existingItem = cartItemRepository.findByShoppingCartIdAndBookId(cart.getId(), bookId);
        if (existingItem.isPresent()) {
            // Update quantity and extend reservation
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            item.extendReservation();
            cart.extendExpiration();
            shoppingCartRepository.save(cart);
            return cartItemRepository.save(item);
        }
        
        // Check if user already owns this book (allow multiple copies in cart)
        // This check is removed to allow multiple purchases
        
        // Get book details
        Book book = bookService.findById(bookId);
        if (book == null) {
            throw new RuntimeException("Book not found");
        }
        
        // Check stock availability (with race condition vulnerability!)
        // Note: This check is intentionally not atomic and can lead to overselling
        int currentStock = book.getStock();
        long reservedCount = cartItemRepository.countActiveReservationsForBook(bookId, Instant.now());
        int availableStock = currentStock - (int) reservedCount;
        
        if (availableStock < quantity) {
            throw new RuntimeException("Insufficient stock. Available: " + availableStock + ", Requested: " + quantity);
        }
        
        // Create cart item with reservation (vulnerability: no atomic stock check)
        String itemId = UUID.randomUUID().toString();
        CartItem item = new CartItem(itemId, cart, bookId, book.getPrice(), quantity);
        
        // Extend cart expiration
        cart.extendExpiration();
        shoppingCartRepository.save(cart);
        
        return cartItemRepository.save(item);
    }

    @Transactional
    public void removeBookFromCart(String userId, String bookId) {
        Optional<ShoppingCart> cart = getActiveCart(userId);
        if (cart.isEmpty()) {
            throw new RuntimeException("No active cart found");
        }
        
        Optional<CartItem> item = cartItemRepository.findByShoppingCartIdAndBookId(cart.get().getId(), bookId);
        if (item.isPresent()) {
            cartItemRepository.delete(item.get());
        }
    }

    public List<CartItem> getCartItems(String userId) {
        Optional<ShoppingCart> cart = getActiveCart(userId);
        if (cart.isEmpty()) {
            return List.of();
        }
        
        List<CartItem> items = cartItemRepository.findByShoppingCartId(cart.get().getId());
        
        // Remove expired reservations
        items.removeIf(item -> {
            if (item.isReservationExpired()) {
                cartItemRepository.delete(item);
                return true;
            }
            return false;
        });
        
        return items;
    }

    public Optional<ShoppingCart> getCheckoutCart(String userId) {
        // Clean up multiple checkout sessions if they exist and return the most recent one
        List<ShoppingCart> checkoutCarts = shoppingCartRepository.findByUserIdAndStatusIn(userId, List.of("CHECKING_OUT"));
        if (checkoutCarts.isEmpty()) {
            return Optional.empty();
        }
        
        // If multiple checkout sessions exist, clean up old ones and keep the most recent
        if (checkoutCarts.size() > 1) {
            System.out.println("Found " + checkoutCarts.size() + " checkout sessions for user " + userId + ", cleaning up duplicates");
            
            // Sort by creation date to get the most recent
            checkoutCarts.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            
            // Keep the most recent one, cancel the rest
            ShoppingCart mostRecent = checkoutCarts.get(0);
            for (int i = 1; i < checkoutCarts.size(); i++) {
                ShoppingCart oldCart = checkoutCarts.get(i);
                oldCart.setStatus("CANCELLED");
                shoppingCartRepository.save(oldCart);
                System.out.println("Cancelled old checkout session: " + oldCart.getId());
            }
            
            return Optional.of(mostRecent);
        }
        
        return Optional.of(checkoutCarts.get(0));
    }

    public List<CartItem> getCartItemsFromCart(String cartId) {
        return cartItemRepository.findByShoppingCartId(cartId);
    }

    @Transactional
    public void startCheckout(String userId) {
        // First, clean up any existing checkout sessions for this user
        List<ShoppingCart> existingCheckouts = shoppingCartRepository.findByUserIdAndStatusIn(userId, List.of("CHECKING_OUT"));
        for (ShoppingCart existingCheckout : existingCheckouts) {
            existingCheckout.setStatus("CANCELLED");
            shoppingCartRepository.save(existingCheckout);
            System.out.println("Cancelled existing checkout session: " + existingCheckout.getId());
        }
        
        Optional<ShoppingCart> cartOpt = getActiveCart(userId);
        if (cartOpt.isEmpty()) {
            throw new RuntimeException("No active cart found");
        }
        
        ShoppingCart cart = cartOpt.get();
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        
        // Change cart status to checking out
        cart.setStatus("CHECKING_OUT");
        
        // Extend all item reservations during checkout
        List<CartItem> items = cartItemRepository.findByShoppingCartId(cart.getId());
        for (CartItem item : items) {
            item.extendReservation();
            cartItemRepository.save(item);
        }
        
        shoppingCartRepository.save(cart);
    }

    @Transactional
    public void completeCheckout(String userId) {
        List<ShoppingCart> checkoutCarts = shoppingCartRepository.findByUserIdAndStatusIn(userId, List.of("CHECKING_OUT"));
        if (checkoutCarts.isEmpty()) {
            throw new RuntimeException("No checkout session found");
        }
        
        // Complete all checkout sessions (in case there are multiple)
        for (ShoppingCart cart : checkoutCarts) {
            cart.setStatus("COMPLETED");
            shoppingCartRepository.save(cart);
            
            // Clean up cart items
            List<CartItem> items = cartItemRepository.findByShoppingCartId(cart.getId());
            cartItemRepository.deleteAll(items);
            
            System.out.println("Completed checkout session: " + cart.getId());
        }
    }

    @Transactional
    public void cancelCheckout(String userId) {
        List<ShoppingCart> checkoutCarts = shoppingCartRepository.findByUserIdAndStatusIn(userId, List.of("CHECKING_OUT"));
        if (checkoutCarts.isEmpty()) {
            return; // Already cancelled or doesn't exist
        }
        
        // Cancel all checkout sessions and restore them to ACTIVE
        for (ShoppingCart cart : checkoutCarts) {
            cart.setStatus("ACTIVE");
            shoppingCartRepository.save(cart);
            System.out.println("Cancelled checkout session: " + cart.getId());
        }
    }

    public void clearCart(String userId) {
        Optional<ShoppingCart> cart = getActiveCart(userId);
        if (cart.isPresent()) {
            List<CartItem> items = cartItemRepository.findByShoppingCartId(cart.get().getId());
            cartItemRepository.deleteAll(items);
            
            cart.get().setStatus("CLEARED");
            shoppingCartRepository.save(cart.get());
        }
    }

    public BigDecimal calculateCartTotal(String userId) {
        List<CartItem> items = getCartItems(userId);
        return items.stream()
                .map(item -> item.getPriceAtAdd().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void updateItemQuantity(String userId, Long itemId, Integer newQuantity) {
        // Convert Long to String for the ID
        Optional<CartItem> itemOpt = cartItemRepository.findById(String.valueOf(itemId));
        if (!itemOpt.isPresent()) {
            throw new RuntimeException("Cart item not found");
        }

        CartItem item = itemOpt.get();
        
        // Verify the item belongs to the user's cart
        Optional<ShoppingCart> cartOpt = getActiveCart(userId);
        if (!cartOpt.isPresent() || !item.getShoppingCart().getId().equals(cartOpt.get().getId())) {
            throw new RuntimeException("Cart item does not belong to user");
        }

        if (newQuantity < 1) {
            throw new RuntimeException("Quantity must be at least 1");
        }

        item.setQuantity(newQuantity);
        cartItemRepository.save(item);
    }

    public void removeCartItem(String userId, Long itemId) {
        // Convert Long to String for the ID
        Optional<CartItem> itemOpt = cartItemRepository.findById(String.valueOf(itemId));
        if (!itemOpt.isPresent()) {
            throw new RuntimeException("Cart item not found");
        }

        CartItem item = itemOpt.get();
        
        // Verify the item belongs to the user's cart
        Optional<ShoppingCart> cartOpt = getActiveCart(userId);
        if (!cartOpt.isPresent() || !item.getShoppingCart().getId().equals(cartOpt.get().getId())) {
            throw new RuntimeException("Cart item does not belong to user");
        }

        cartItemRepository.delete(item);
    }

    // Clean up expired carts and reservations
    @Transactional
    public void cleanupExpiredCarts() {
        Instant now = Instant.now();
        
        // Clean up expired cart items
        List<CartItem> expiredItems = cartItemRepository.findExpiredReservations(now);
        cartItemRepository.deleteAll(expiredItems);
        
        // Clean up expired carts
        List<ShoppingCart> expiredCarts = shoppingCartRepository.findExpiredCarts(now);
        for (ShoppingCart cart : expiredCarts) {
            cart.setStatus("EXPIRED");
            shoppingCartRepository.save(cart);
        }
    }
}
