package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AddEditProductDialog
import com.example.ui.components.AuthDialog
import com.example.ui.components.CheckoutDialog
import com.example.ui.components.CreateBuyerDialog
import com.example.ui.components.OrderSuccessDialog
import com.example.ui.components.ProductDetailDialog
import com.example.ui.screens.AccountSettingsScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.SellerHubScreen
import com.example.ui.screens.ShopScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSellerApp(
    viewModel: ProductViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val sellerStats by viewModel.sellerStats.collectAsStateWithLifecycle()
    val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val buyers by viewModel.buyers.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    val activeTelegramHandle = appSettings?.telegramUsername?.ifBlank { "apex_support" }
        ?: (loggedInUser?.telegramUsername?.ifBlank { "apex_support" } ?: "apex_support")

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val totalCartCount = cartItems.sumOf { it.cartItem.quantity }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = appSettings?.storeName ?: "Product Seller",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    val isUserLoggedIn = loggedInUser != null && loggedInUser!!.isLoggedIn
                    val isUserAdmin = isUserLoggedIn && (loggedInUser?.isAdmin == true)
                    val isUserBuyer = isUserLoggedIn && (loggedInUser?.isBuyer == true)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            isUserAdmin -> MaterialTheme.colorScheme.primaryContainer
                            isUserBuyer -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .testTag("top_auth_status_button")
                            .clickable {
                                if (isUserLoggedIn) {
                                    viewModel.setTab(AppTab.SETTINGS)
                                } else {
                                    viewModel.openAuthDialog("LOGIN")
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isUserAdmin -> Icons.Default.AdminPanelSettings
                                    isUserBuyer -> Icons.Default.Person
                                    else -> Icons.Default.Lock
                                },
                                contentDescription = "Account",
                                tint = when {
                                    isUserAdmin -> MaterialTheme.colorScheme.primary
                                    isUserBuyer -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    isUserAdmin -> "Admin"
                                    isUserBuyer -> loggedInUser?.fullName?.take(8) ?: "Buyer"
                                    else -> "Sign In"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isUserAdmin -> MaterialTheme.colorScheme.primary
                                    isUserBuyer -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setTab(AppTab.CART) },
                        modifier = Modifier.testTag("top_cart_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (totalCartCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    ) {
                                        Text("$totalCartCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Shopping Cart",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                // 1. Shop Tab
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.SHOP,
                    onClick = { viewModel.setTab(AppTab.SHOP) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == AppTab.SHOP) Icons.Filled.Storefront else Icons.Outlined.Storefront,
                            contentDescription = "Market"
                        )
                    },
                    label = { Text(AppTab.SHOP.title, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_shop"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // 2. Seller Hub Tab
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.SELLER_HUB,
                    onClick = { viewModel.setTab(AppTab.SELLER_HUB) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == AppTab.SELLER_HUB) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                            contentDescription = "Seller Hub"
                        )
                    },
                    label = { Text(AppTab.SELLER_HUB.title, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_seller_hub"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // 3. Cart Tab
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.CART,
                    onClick = { viewModel.setTab(AppTab.CART) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (totalCartCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ) {
                                        Text("$totalCartCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.currentTab == AppTab.CART) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                                contentDescription = "Cart"
                            )
                        }
                    },
                    label = { Text(AppTab.CART.title, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_cart"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // 4. Orders Tab
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.ORDERS,
                    onClick = { viewModel.setTab(AppTab.ORDERS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == AppTab.ORDERS) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = "Orders"
                        )
                    },
                    label = { Text(AppTab.ORDERS.title, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_orders"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // 5. Settings Tab
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.SETTINGS,
                    onClick = { viewModel.setTab(AppTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == AppTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text(AppTab.SETTINGS.title, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_settings"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            if (uiState.currentTab == AppTab.SELLER_HUB) {
                FloatingActionButton(
                    onClick = {
                        if (loggedInUser != null && loggedInUser!!.isLoggedIn) {
                            viewModel.openAddProductDialog()
                        } else {
                            viewModel.openAuthDialog("LOGIN")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("seller_fab_add_product")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Product")
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "tab_switch_animation"
        ) { tab ->
            when (tab) {
                AppTab.SHOP -> {
                    ShopScreen(
                        products = filteredProducts,
                        searchQuery = uiState.searchQuery,
                        selectedCategory = uiState.selectedCategory,
                        sortOption = uiState.sortOption,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onCategorySelect = viewModel::setSelectedCategory,
                        onSortOptionSelect = viewModel::setSortOption,
                        onProductClick = viewModel::openProductDetail,
                        onAddToCart = { viewModel.addToCart(it.id, 1) },
                        telegramUsername = activeTelegramHandle
                    )
                }

                AppTab.SELLER_HUB -> {
                    SellerHubScreen(
                        sellerStats = sellerStats,
                        products = allProducts,
                        user = loggedInUser,
                        buyers = buyers,
                        onAddProductClick = viewModel::openAddProductDialog,
                        onEditProductClick = viewModel::openEditProductDialog,
                        onDeleteProductClick = viewModel::deleteProduct,
                        onUpdateStock = viewModel::updateStock,
                        onToggleListing = viewModel::toggleListingStatus,
                        onOpenAuthDialog = viewModel::openAuthDialog,
                        onOpenSettings = { viewModel.setTab(AppTab.SETTINGS) },
                        onOpenCreateBuyerDialog = viewModel::openCreateBuyerDialog,
                        onDeleteBuyer = viewModel::deleteBuyerUser,
                        onSwitchToBuyer = { id, name ->
                            viewModel.switchToUser(id, name)
                        }
                    )
                }

                AppTab.CART -> {
                    CartScreen(
                        cartItems = cartItems,
                        onUpdateQuantity = viewModel::updateCartQuantity,
                        onRemoveItem = viewModel::removeFromCart,
                        onClearCart = viewModel::clearCart,
                        onProceedToCheckout = viewModel::openCheckoutModal,
                        onStartShopping = { viewModel.setTab(AppTab.SHOP) }
                    )
                }

                AppTab.ORDERS -> {
                    OrdersScreen(
                        orders = orders
                    )
                }

                AppTab.SETTINGS -> {
                    AccountSettingsScreen(
                        user = loggedInUser,
                        settings = appSettings ?: com.example.data.model.AppSettingsEntity(),
                        buyers = buyers,
                        onOpenAuthDialog = viewModel::openAuthDialog,
                        onLogout = viewModel::logoutUser,
                        onUpdateProfile = viewModel::updateUserProfile,
                        onUpdateSettings = viewModel::updateAppSettings,
                        onOpenCreateBuyerDialog = viewModel::openCreateBuyerDialog,
                        onDeleteBuyer = viewModel::deleteBuyerUser,
                        onSwitchToBuyer = { id, name ->
                            viewModel.switchToUser(id, name)
                        },
                        onShopProducts = { viewModel.setTab(AppTab.SHOP) }
                    )
                }
            }
        }
    }

    // Product Detail Dialog
    uiState.selectedProductForDetail?.let { product ->
        ProductDetailDialog(
            product = product,
            onDismiss = viewModel::closeProductDetail,
            onAddToCart = { qty -> viewModel.addToCart(product.id, qty) },
            onBuyNow = { qty ->
                viewModel.addToCart(product.id, qty)
                viewModel.setTab(AppTab.CART)
                viewModel.openCheckoutModal()
            },
            telegramUsername = activeTelegramHandle
        )
    }

    // Add / Edit Product Dialog
    if (uiState.isAddEditProductOpen) {
        AddEditProductDialog(
            productToEdit = uiState.productToEdit,
            onDismiss = viewModel::closeAddEditProductDialog,
            onSave = { title, desc, price, originalPrice, cat, stock, img, sku, featured ->
                viewModel.saveProduct(
                    title = title,
                    description = desc,
                    price = price,
                    originalPrice = originalPrice,
                    category = cat,
                    stockQuantity = stock,
                    imageResName = img,
                    sku = sku,
                    isFeatured = featured
                )
            }
        )
    }

    // Auth Dialog (Login / Register)
    if (uiState.isAuthDialogOpen) {
        AuthDialog(
            initialMode = uiState.authDialogMode,
            onDismiss = viewModel::closeAuthDialog,
            onLogin = { username, password, callback ->
                viewModel.loginUser(username, password, callback)
            },
            onResetPassword = { email, newPassword, callback ->
                viewModel.resetPasswordByEmail(email, newPassword, callback)
            },
            onRegister = { username, email, password, fullName, storeName, telegram, phone, bio, callback ->
                viewModel.registerUser(
                    username = username,
                    email = email,
                    password = password,
                    fullName = fullName,
                    storeName = storeName,
                    telegramUsername = telegram,
                    phoneNumber = phone,
                    bio = bio,
                    onResult = callback
                )
            }
        )
    }

    // Checkout Dialog
    if (uiState.isCheckoutModalOpen) {
        val subtotal = cartItems.sumOf { it.product.price * it.cartItem.quantity }
        val discount = if (subtotal > 100.0) subtotal * 0.10 else 0.0
        val estimatedTax = (subtotal - discount) * 0.08
        val total = (subtotal - discount) + estimatedTax

        CheckoutDialog(
            cartItems = cartItems,
            subtotal = subtotal,
            discountAmount = discount,
            estimatedTax = estimatedTax,
            totalAmount = total,
            currentUser = loggedInUser,
            onDismiss = viewModel::closeCheckoutModal,
            onConfirmOrder = { name, address, paymentMethod ->
                viewModel.placeOrder(
                    customerName = name,
                    address = address,
                    paymentMethod = paymentMethod,
                    cartItems = cartItems,
                    totalAmount = total
                )
            }
        )
    }

    // Create Buyer User Dialog (Admin feature)
    if (uiState.isCreateBuyerDialogOpen) {
        CreateBuyerDialog(
            onDismiss = viewModel::closeCreateBuyerDialog,
            onCreateBuyer = { fullName, username, email, password, phone, address, loginImmediately, callback ->
                viewModel.createBuyerUser(
                    fullName = fullName,
                    username = username,
                    email = email,
                    password = password,
                    phoneNumber = phone,
                    shippingAddress = address,
                    loginImmediately = loginImmediately,
                    onResult = callback
                )
            }
        )
    }

    // Order Success Dialog
    if (uiState.isOrderSuccessDialogOpen && uiState.lastPlacedOrder != null) {
        OrderSuccessDialog(
            order = uiState.lastPlacedOrder!!,
            onDismiss = viewModel::closeOrderSuccessDialog
        )
    }
}
