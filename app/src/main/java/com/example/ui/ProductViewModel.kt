package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CartItemWithProduct
import com.example.data.model.OrderEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SellerStats
import com.example.data.model.UserEntity
import com.example.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    SHOP("Market"),
    SELLER_HUB("Seller Hub"),
    CART("Cart"),
    ORDERS("Orders"),
    SETTINGS("Settings")
}

enum class SortOption(val label: String) {
    FEATURED("Featured"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    HIGHEST_RATED("Top Rated"),
    POPULARITY("Best Sellers")
}

data class UiState(
    val currentTab: AppTab = AppTab.SHOP,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val sortOption: SortOption = SortOption.FEATURED,
    val selectedProductForDetail: ProductEntity? = null,
    val isAddEditProductOpen: Boolean = false,
    val productToEdit: ProductEntity? = null,
    val isCheckoutModalOpen: Boolean = false,
    val lastPlacedOrder: OrderEntity? = null,
    val isOrderSuccessDialogOpen: Boolean = false,
    val isAuthDialogOpen: Boolean = false,
    val authDialogMode: String = "LOGIN",
    val snackbarMessage: String? = null
)

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductRepository

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = ProductRepository(
            productDao = database.productDao(),
            cartDao = database.cartDao(),
            orderDao = database.orderDao(),
            userDao = database.userDao(),
            appSettingsDao = database.appSettingsDao()
        )
    }

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listedProducts: StateFlow<List<ProductEntity>> = repository.listedProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartItems: StateFlow<List<CartItemWithProduct>> = repository.cartItemsWithProduct
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loggedInUser: StateFlow<UserEntity?> = repository.loggedInUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val appSettings: StateFlow<AppSettingsEntity?> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sellerStats: StateFlow<SellerStats> = repository.sellerStats
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SellerStats(0.0, 0, 0, 0, "N/A")
        )

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        repository.listedProducts,
        _uiState
    ) { products, state ->
        var list = products

        // Filter category
        if (state.selectedCategory != "All") {
            list = list.filter { it.category.equals(state.selectedCategory, ignoreCase = true) }
        }

        // Filter search query
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(query) ||
                        it.description.lowercase().contains(query) ||
                        it.category.lowercase().contains(query)
            }
        }

        // Sort
        when (state.sortOption) {
            SortOption.FEATURED -> list.sortedWith(compareByDescending<ProductEntity> { it.isFeatured }.thenByDescending { it.id })
            SortOption.PRICE_LOW_HIGH -> list.sortedBy { it.price }
            SortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.price }
            SortOption.HIGHEST_RATED -> list.sortedByDescending { it.rating }
            SortOption.POPULARITY -> list.sortedByDescending { it.salesCount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun openAuthDialog(mode: String = "LOGIN") {
        _uiState.update { it.copy(isAuthDialogOpen = true, authDialogMode = mode) }
    }

    fun closeAuthDialog() {
        _uiState.update { it.copy(isAuthDialogOpen = false) }
    }

    fun loginUser(username: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = repository.authenticateUser(username, password)
            if (user != null) {
                _uiState.update { it.copy(isAuthDialogOpen = false) }
                showSnackbar("Welcome back, ${user.fullName.ifBlank { user.username }}!")
                onResult(true, "Logged in successfully")
            } else {
                onResult(false, "Invalid username or password")
            }
        }
    }

    fun registerUser(
        username: String,
        email: String,
        password: String,
        fullName: String,
        storeName: String,
        telegramUsername: String,
        phoneNumber: String = "+1 (555) 234-5678",
        bio: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.registerUser(
                username = username,
                email = email,
                password = password,
                fullName = fullName,
                storeName = storeName,
                telegramUsername = telegramUsername,
                phoneNumber = phoneNumber,
                bio = bio
            )
            if (result.first) {
                _uiState.update { it.copy(isAuthDialogOpen = false) }
                showSnackbar("Account created! Welcome, $fullName")
                onResult(true, result.second)
            } else {
                onResult(false, result.second)
            }
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            repository.logoutUser()
            showSnackbar("You have been logged out")
        }
    }

    fun updateUserProfile(user: UserEntity) {
        viewModelScope.launch {
            repository.updateUserProfile(user)
            showSnackbar("Profile updated successfully")
        }
    }

    fun updateAppSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.updateAppSettings(settings)
            showSnackbar("Store settings saved")
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSelectedCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setSortOption(sortOption: SortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
    }

    fun openProductDetail(product: ProductEntity) {
        _uiState.update { it.copy(selectedProductForDetail = product) }
    }

    fun closeProductDetail() {
        _uiState.update { it.copy(selectedProductForDetail = null) }
    }

    fun openAddProductDialog() {
        _uiState.update { it.copy(isAddEditProductOpen = true, productToEdit = null) }
    }

    fun openEditProductDialog(product: ProductEntity) {
        _uiState.update { it.copy(isAddEditProductOpen = true, productToEdit = product) }
    }

    fun closeAddEditProductDialog() {
        _uiState.update { it.copy(isAddEditProductOpen = false, productToEdit = null) }
    }

    fun saveProduct(
        title: String,
        description: String,
        price: Double,
        originalPrice: Double?,
        category: String,
        stockQuantity: Int,
        imageResName: String,
        sku: String,
        isFeatured: Boolean
    ) {
        viewModelScope.launch {
            val currentEdit = _uiState.value.productToEdit
            if (currentEdit != null) {
                val updated = currentEdit.copy(
                    title = title,
                    description = description,
                    price = price,
                    originalPrice = originalPrice,
                    category = category,
                    stockQuantity = stockQuantity,
                    imageResName = imageResName,
                    sku = sku,
                    isFeatured = isFeatured
                )
                repository.updateProduct(updated)
                showSnackbar("Product updated successfully")
            } else {
                val newProduct = ProductEntity(
                    title = title,
                    description = description,
                    price = price,
                    originalPrice = originalPrice,
                    category = category,
                    stockQuantity = stockQuantity,
                    imageResName = imageResName,
                    sku = sku.ifBlank { "SKU-" + (1000..9999).random() },
                    isFeatured = isFeatured,
                    isListed = true
                )
                repository.insertProduct(newProduct)
                showSnackbar("New product listed for sale!")
            }
            closeAddEditProductDialog()
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            repository.deleteProduct(id)
            showSnackbar("Product removed from store")
        }
    }

    fun updateStock(id: Long, newStock: Int) {
        viewModelScope.launch {
            repository.updateStock(id, newStock.coerceAtLeast(0))
        }
    }

    fun toggleListingStatus(id: Long, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.updateListingStatus(id, !currentStatus)
            showSnackbar(if (!currentStatus) "Product is now live" else "Product unlisted")
        }
    }

    fun addToCart(productId: Long, quantity: Int = 1) {
        viewModelScope.launch {
            repository.addToCart(productId, quantity)
            showSnackbar("Added to cart")
        }
    }

    fun updateCartQuantity(cartItemId: Long, newQuantity: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(cartItemId, newQuantity)
        }
    }

    fun removeFromCart(cartItemId: Long) {
        viewModelScope.launch {
            repository.removeFromCart(cartItemId)
            showSnackbar("Item removed from cart")
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
            showSnackbar("Cart cleared")
        }
    }

    fun openCheckoutModal() {
        _uiState.update { it.copy(isCheckoutModalOpen = true) }
    }

    fun closeCheckoutModal() {
        _uiState.update { it.copy(isCheckoutModalOpen = false) }
    }

    fun placeOrder(
        customerName: String,
        address: String,
        paymentMethod: String,
        cartItems: List<CartItemWithProduct>,
        totalAmount: Double
    ) {
        viewModelScope.launch {
            val order = repository.processCheckout(
                customerName = customerName,
                address = address,
                paymentMethod = paymentMethod,
                cartItems = cartItems,
                totalAmount = totalAmount
            )
            _uiState.update {
                it.copy(
                    isCheckoutModalOpen = false,
                    lastPlacedOrder = order,
                    isOrderSuccessDialogOpen = true,
                    currentTab = AppTab.ORDERS
                )
            }
        }
    }

    fun closeOrderSuccessDialog() {
        _uiState.update { it.copy(isOrderSuccessDialogOpen = false) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
