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
        val user = userDao.authenticate(identifier.trim(), password)
        if (user != null) {
            userDao.logoutAllUsers()
            userDao.setLoggedIn(user.id)
            return user.copy(isLoggedIn = true)
        }
        return null
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
}
