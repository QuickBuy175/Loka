package com.example.data.repository

import com.example.data.local.AppSettingsDao
import com.example.data.local.CartDao
import com.example.data.local.OrderDao
import com.example.data.local.ProductDao
import com.example.data.local.UserDao
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CartItemEntity
import com.example.data.model.CartItemWithProduct
import com.example.data.model.OrderEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SellerStats
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProductRepository(
    private val productDao: ProductDao,
    private val cartDao: CartDao,
    private val orderDao: OrderDao,
    private val userDao: UserDao,
    private val appSettingsDao: AppSettingsDao
) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val listedProducts: Flow<List<ProductEntity>> = productDao.getListedProducts()
    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()
    val loggedInUser: Flow<UserEntity?> = userDao.getLoggedInUser()
    val allBuyers: Flow<List<UserEntity>> = userDao.getAllBuyers()
    val appSettings: Flow<AppSettingsEntity?> = appSettingsDao.getSettings()

    val cartItemsWithProduct: Flow<List<CartItemWithProduct>> = combine(
        cartDao.getAllCartItems(),
        productDao.getAllProducts()
    ) { cartItems, products ->
        val productMap = products.associateBy { it.id }
        cartItems.mapNotNull { item ->
            productMap[item.productId]?.let { product ->
                CartItemWithProduct(cartItem = item, product = product)
            }
        }
    }

    val sellerStats: Flow<SellerStats> = combine(
        productDao.getAllProducts(),
        orderDao.getAllOrders()
    ) { products, orders ->
        val revenue = orders.sumOf { it.totalAmount }
        val activeCount = products.count { it.isListed }
        val totalStock = products.sumOf { it.stockQuantity }
        val topProduct = products.maxByOrNull { it.salesCount }?.title ?: "N/A"

        SellerStats(
            totalRevenue = revenue,
            totalOrders = orders.size,
            activeListings = activeCount,
            totalInventoryCount = totalStock,
            topSellingProduct = topProduct
        )
    }

    suspend fun insertProduct(product: ProductEntity): Long {
        return productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(id: Long) {
        cartDao.deleteByProductId(id)
        productDao.deleteProductById(id)
    }

    suspend fun updateStock(id: Long, newStock: Int) {
        productDao.updateStock(id, newStock)
    }

    suspend fun updateListingStatus(id: Long, isListed: Boolean) {
        productDao.updateListingStatus(id, isListed)
    }

    suspend fun addToCart(productId: Long, quantity: Int = 1) {
        val existing = cartDao.getCartItemByProductId(productId)
        if (existing != null) {
            cartDao.updateQuantity(existing.id, existing.quantity + quantity)
        } else {
            cartDao.insertCartItem(
                CartItemEntity(
                    productId = productId,
                    quantity = quantity
                )
            )
        }
    }

    suspend fun updateCartQuantity(cartItemId: Long, quantity: Int) {
        if (quantity <= 0) {
            cartDao.deleteById(cartItemId)
        } else {
            cartDao.updateQuantity(cartItemId, quantity)
        }
    }

    suspend fun removeFromCart(cartItemId: Long) {
        cartDao.deleteById(cartItemId)
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }

    suspend fun processCheckout(
        customerName: String,
        address: String,
        paymentMethod: String,
        cartItems: List<CartItemWithProduct>,
        totalAmount: Double
    ): OrderEntity {
        // Record sale and deduct stock for each product
        cartItems.forEach { item ->
            productDao.recordSale(item.product.id, item.cartItem.quantity)
        }

        // Build item summary
        val itemsSummary = cartItems.joinToString(", ") {
            "${it.product.title} (x${it.cartItem.quantity})"
        }
        val totalCount = cartItems.sumOf { it.cartItem.quantity }

        val orderNumber = "ORD-" + UUID.randomUUID().toString().take(6).uppercase()
        val order = OrderEntity(
            orderNumber = orderNumber,
            itemsSummary = itemsSummary,
            totalAmount = totalAmount,
            itemsCount = totalCount,
            paymentMethod = paymentMethod,
            orderStatus = "Processing",
            customerName = customerName.ifBlank { "Guest Shopper" },
            customerAddress = address.ifBlank { "Standard Delivery Address" }
        )

        orderDao.insertOrder(order)
        cartDao.clearCart()
        return order
    }

    suspend fun authenticateUser(identifier: String, password: String): UserEntity? {
        val trimmedIdentifier = identifier.trim()
        val user = userDao.authenticate(trimmedIdentifier, password)
        if (user != null) {
            userDao.logoutAllUsers()
            userDao.setLoggedIn(user.id)
            return user.copy(isLoggedIn = true)
        }

        // Support admin login with Admin175 or prasith1980@gmail.com and password @1234567
        val isAdminIdentifier = trimmedIdentifier.equals("Admin175", ignoreCase = true) ||
                trimmedIdentifier.equals("prasith1980@gmail.com", ignoreCase = true) ||
                trimmedIdentifier.equals("admin", ignoreCase = true) ||
                trimmedIdentifier.equals("admin@apexstore.com", ignoreCase = true)

        if (isAdminIdentifier && password == "@1234567") {
            val existing = userDao.findExistingUser("prasith1980@gmail.com", "Admin175")
                ?: userDao.findExistingUser("admin@apexstore.com", "admin")
            if (existing != null) {
                val updatedAdmin = existing.copy(
                    username = "Admin175",
                    email = "prasith1980@gmail.com",
                    password = "@1234567",
                    isLoggedIn = true
                )
                userDao.logoutAllUsers()
                userDao.updateUser(updatedAdmin)
                userDao.setLoggedIn(updatedAdmin.id)
                return updatedAdmin
            } else {
                val adminUser = UserEntity(
                    username = "Admin175",
                    email = "prasith1980@gmail.com",
                    password = "@1234567",
                    fullName = "Store Admin",
                    storeName = "Apex Studio Store",
                    telegramUsername = "apex_support",
                    phoneNumber = "+1 (555) 234-5678",
                    bio = "Official Store Administrator & Inventory Curator.",
                    isLoggedIn = true
                )
                userDao.logoutAllUsers()
                val newId = userDao.insertUser(adminUser)
                return adminUser.copy(id = newId)
            }
        }

        return null
    }

    suspend fun resetPasswordByEmail(email: String, newPassword: String): Pair<Boolean, String> {
        val cleanEmail = email.trim()
        val isAuthorizedEmail = cleanEmail.equals("prasith1980@gmail.com", ignoreCase = true) ||
                cleanEmail.equals("admin@apexstore.com", ignoreCase = true)

        if (!isAuthorizedEmail) {
            val existing = userDao.findExistingUser(cleanEmail, "")
            if (existing == null) {
                return Pair(false, "No administrator found matching this email address.")
            }
        }

        val adminUser = userDao.findExistingUser("prasith1980@gmail.com", "Admin175")
            ?: userDao.findExistingUser("admin@apexstore.com", "admin")
            ?: userDao.findExistingUser(cleanEmail, "")

        if (adminUser != null) {
            val updated = adminUser.copy(
                email = "prasith1980@gmail.com",
                username = "Admin175",
                password = newPassword.ifBlank { "@1234567" }
            )
            userDao.updateUser(updated)
            return Pair(true, "Password updated successfully for $cleanEmail.")
        } else {
            val newAdmin = UserEntity(
                username = "Admin175",
                email = "prasith1980@gmail.com",
                password = newPassword.ifBlank { "@1234567" },
                fullName = "Store Admin",
                storeName = "Apex Studio Store",
                telegramUsername = "apex_support",
                phoneNumber = "+1 (555) 234-5678",
                bio = "Official Store Administrator & Inventory Curator.",
                isLoggedIn = false
            )
            userDao.insertUser(newAdmin)
            return Pair(true, "Password updated successfully for $cleanEmail.")
        }
    }

    suspend fun registerUser(
        username: String,
        email: String,
        password: String,
        fullName: String,
        storeName: String,
        telegramUsername: String,
        phoneNumber: String,
        bio: String
    ): Pair<Boolean, String> {
        val existing = userDao.findExistingUser(email.trim(), username.trim())
        if (existing != null) {
            return Pair(false, "User with this email or username already exists.")
        }
        val cleanTelegram = telegramUsername.trim().removePrefix("@")
        val newUser = UserEntity(
            username = username.trim(),
            email = email.trim(),
            password = password,
            fullName = fullName.trim(),
            storeName = storeName.ifBlank { "My Seller Store" }.trim(),
            telegramUsername = cleanTelegram.ifBlank { "apex_support" },
            phoneNumber = phoneNumber.trim(),
            bio = bio.trim(),
            isLoggedIn = true
        )
        userDao.logoutAllUsers()
        val newId = userDao.insertUser(newUser)

        // Also update app settings store name & telegram
        val currentSettings = appSettingsDao.getSettingsSync()
        if (currentSettings != null) {
            appSettingsDao.saveSettings(
                currentSettings.copy(
                    storeName = newUser.storeName,
                    telegramUsername = newUser.telegramUsername,
                    supportEmail = newUser.email,
                    supportPhone = newUser.phoneNumber
                )
            )
        }

        return Pair(true, "Account created successfully!")
    }

    suspend fun logoutUser() {
        userDao.logoutAllUsers()
    }

    suspend fun updateUserProfile(user: UserEntity) {
        userDao.updateUser(user)
        val currentSettings = appSettingsDao.getSettingsSync()
        if (currentSettings != null) {
            appSettingsDao.saveSettings(
                currentSettings.copy(
                    storeName = user.storeName,
                    telegramUsername = user.telegramUsername.removePrefix("@"),
                    supportEmail = user.email,
                    supportPhone = user.phoneNumber
                )
            )
        }
    }

    suspend fun updateAppSettings(settings: AppSettingsEntity) {
        appSettingsDao.saveSettings(settings)
    }

    suspend fun getAppSettingsSync(): AppSettingsEntity {
        return appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
    }

    suspend fun createBuyerUser(
        fullName: String,
        username: String,
        email: String,
        password: String,
        phoneNumber: String,
        shippingAddress: String,
        loginImmediately: Boolean = false
    ): Pair<Boolean, String> {
        val cleanUsername = username.trim()
        val cleanEmail = email.trim()

        if (cleanUsername.isBlank()) {
            return Pair(false, "Please enter a valid username for the buyer.")
        }
        if (cleanEmail.isBlank()) {
            return Pair(false, "Please enter a valid email address.")
        }
        if (password.isBlank()) {
            return Pair(false, "Please enter a password for the buyer.")
        }

        val existing = userDao.findExistingUser(cleanEmail, cleanUsername)
        if (existing != null) {
            return Pair(false, "A user with username '$cleanUsername' or email '$cleanEmail' already exists.")
        }

        val buyerUser = UserEntity(
            username = cleanUsername,
            email = cleanEmail,
            password = password,
            fullName = fullName.trim().ifBlank { cleanUsername },
            storeName = "Customer",
            telegramUsername = cleanUsername,
            phoneNumber = phoneNumber.trim().ifBlank { "+1 (555) 000-0000" },
            bio = "Registered buyer account.",
            role = "BUYER",
            shippingAddress = shippingAddress.trim().ifBlank { "Standard Delivery Address" },
            isLoggedIn = loginImmediately
        )

        if (loginImmediately) {
            userDao.logoutAllUsers()
        }

        val newId = userDao.insertUser(buyerUser)
        if (loginImmediately) {
            userDao.setLoggedIn(newId)
        }

        return Pair(true, "Buyer account for '${buyerUser.fullName}' created successfully! They can now log in and buy products.")
    }

    suspend fun deleteBuyerUser(userId: Long): Pair<Boolean, String> {
        userDao.deleteUser(userId)
        return Pair(true, "Buyer account deleted successfully.")
    }

    suspend fun switchToUser(userId: Long) {
        userDao.logoutAllUsers()
        userDao.setLoggedIn(userId)
    }
}
