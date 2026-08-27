package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CartItemEntity
import com.example.data.model.OrderEntity
import com.example.data.model.ProductEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ProductEntity::class, CartItemEntity::class, OrderEntity::class, UserEntity::class, AppSettingsEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun userDao(): UserDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "product_seller_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(
                        database.productDao(),
                        database.orderDao(),
                        database.userDao(),
                        database.appSettingsDao()
                    )
                }
            }
        }
    }
}

suspend fun populateInitialData(
    productDao: ProductDao,
    orderDao: OrderDao,
    userDao: UserDao,
    appSettingsDao: AppSettingsDao
) {
    if (productDao.getProductCount() > 0) return

    // Seed default admin/seller user
    val defaultSeller = UserEntity(
        username = "apex_seller",
        email = "seller@apexstore.com",
        password = "password123",
        fullName = "Alex Mercer",
        storeName = "Apex Studio Store",
        telegramUsername = "apex_support",
        phoneNumber = "+1 (555) 234-5678",
        bio = "Lead Merchant & Curator at Apex Studio. High-grade electronics, fashion & curated accessories.",
        isLoggedIn = true
    )
    userDao.insertUser(defaultSeller)

    // Seed default app/store settings
    val defaultSettings = AppSettingsEntity(
        storeName = "Apex Studio Store",
        storeTagline = "Premium curated electronics & modern lifestyle essentials",
        telegramUsername = "apex_support",
        supportEmail = "support@apexstore.com",
        supportPhone = "+1 (555) 234-5678",
        currencySymbol = "$",
        taxRate = 8.0,
        orderNotificationEnabled = true,
        autoAcceptOrders = true
    )
    appSettingsDao.saveSettings(defaultSettings)

    val sampleProducts = listOf(
        ProductEntity(
            title = "Apex Wireless ANC Headphones",
            description = "Flagship active noise-canceling headphones featuring 40mm beryllium drivers, 40-hour ultra-long battery life, multi-point Bluetooth 5.3, and custom equalizer profiles.",
            price = 149.99,
            originalPrice = 199.99,
            category = "Electronics",
            stockQuantity = 24,
            imageResName = "img_product_headphone",
            sku = "APX-HD-01",
            rating = 4.9,
            reviewsCount = 128,
            isFeatured = true,
            isListed = true,
            sellerName = "Apex Studio",
            sellerRating = 4.9,
            salesCount = 54
        ),
        ProductEntity(
            title = "Horizon Chrono Smartwatch",
            description = "Sleek aerospace titanium smartwatch equipped with bright AMOLED always-on display, heart rate & SpO2 sensors, GPS routing, and 7-day battery resilience.",
            price = 199.99,
            originalPrice = 249.99,
            category = "Electronics",
            stockQuantity = 15,
            imageResName = "img_product_watch",
            sku = "HRZ-SW-04",
            rating = 4.8,
            reviewsCount = 94,
            isFeatured = true,
            isListed = true,
            sellerName = "Apex Studio",
            sellerRating = 4.9,
            salesCount = 38
        ),
        ProductEntity(
            title = "AeroGlide Runner Sneakers",
            description = "Modern lightweight athletic footwear engineered with breathable dynamic knit mesh, high-rebound responsive midsole cushioning, and grippy rubber traction soles.",
            price = 89.99,
            originalPrice = 120.00,
            category = "Fashion",
            stockQuantity = 30,
            imageResName = "img_product_sneakers",
            sku = "AER-SN-09",
            rating = 4.7,
            reviewsCount = 76,
            isFeatured = true,
            isListed = true,
            sellerName = "Stride Essentials",
            sellerRating = 4.8,
            salesCount = 62
        ),
        ProductEntity(
            title = "Minimalist Waxed Canvas Backpack",
            description = "Water-resistant weatherproof canvas commuter pack with full-grain leather accents, secure padded 16-inch laptop pocket, and quick-access magnetic closures.",
            price = 74.50,
            originalPrice = 95.00,
            category = "Fashion",
            stockQuantity = 18,
            imageResName = "img_product_sneakers",
            sku = "BAG-CX-12",
            rating = 4.8,
            reviewsCount = 45,
            isFeatured = false,
            isListed = true,
            sellerName = "Apex Studio",
            sellerRating = 4.9,
            salesCount = 29
        ),
        ProductEntity(
            title = "Compact Mechanical Keyboard 75%",
            description = "Hot-swappable tactile mechanical switches with factory-lubed stabilizers, sound-dampening silicone gaskets, customizable RGB backlighting, and CNC aluminum casing.",
            price = 119.00,
            originalPrice = 139.99,
            category = "Electronics",
            stockQuantity = 12,
            imageResName = "img_product_headphone",
            sku = "KBD-MK-75",
            rating = 4.9,
            reviewsCount = 112,
            isFeatured = false,
            isListed = true,
            sellerName = "Apex Studio",
            sellerRating = 4.9,
            salesCount = 47
        ),
        ProductEntity(
            title = "Handcrafted Ceramic Mug Set",
            description = "Set of 2 artisan ceramic coffee mugs with comfortable ergonomic handles, matte stone glaze, and excellent heat retention for your morning brew.",
            price = 32.00,
            originalPrice = 42.00,
            category = "Home & Kitchen",
            stockQuantity = 45,
            imageResName = "img_product_watch",
            sku = "HOM-MG-02",
            rating = 4.6,
            reviewsCount = 53,
            isFeatured = false,
            isListed = true,
            sellerName = "Clay & Stone Co.",
            sellerRating = 4.7,
            salesCount = 81
        ),
        ProductEntity(
            title = "Nordic Touch Dimmable Desk Lamp",
            description = "Architectural LED task lamp featuring stepless touch brightness control, warm to daylight color temp switching, and built-in 15W Qi wireless charger base.",
            price = 58.00,
            originalPrice = 75.00,
            category = "Home & Kitchen",
            stockQuantity = 22,
            imageResName = "img_product_watch",
            sku = "LMP-ND-08",
            rating = 4.7,
            reviewsCount = 38,
            isFeatured = false,
            isListed = true,
            sellerName = "Apex Studio",
            sellerRating = 4.9,
            salesCount = 19
        )
    )

    productDao.insertAll(sampleProducts)

    // Seed sample completed order
    val initialOrder = OrderEntity(
        orderNumber = "ORD-8921",
        itemsSummary = "Apex Wireless ANC Headphones (x1), AeroGlide Runner Sneakers (x1)",
        totalAmount = 239.98,
        itemsCount = 2,
        paymentMethod = "Credit Card (•••• 4242)",
        orderStatus = "Delivered",
        customerName = "Alex Morgan",
        customerAddress = "742 Evergreen Terrace, Springfield",
        timestamp = System.currentTimeMillis() - 86400000L * 2
    )
    orderDao.insertOrder(initialOrder)
}
