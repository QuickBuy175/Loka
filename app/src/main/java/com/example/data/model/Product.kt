package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val price: Double,
    val originalPrice: Double? = null,
    val category: String,
    val stockQuantity: Int,
    val imageResName: String,
    val sku: String = "",
    val rating: Double = 4.8,
    val reviewsCount: Int = 32,
    val isFeatured: Boolean = false,
    val isListed: Boolean = true,
    val sellerName: String = "Apex Store",
    val sellerRating: Double = 4.9,
    val salesCount: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val quantity: Int = 1,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String,
    val itemsSummary: String,
    val totalAmount: Double,
    val itemsCount: Int,
    val paymentMethod: String,
    val orderStatus: String, // "Processing", "Shipped", "Delivered"
    val customerName: String,
    val customerAddress: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CartItemWithProduct(
    val cartItem: CartItemEntity,
    val product: ProductEntity
)

data class SellerStats(
    val totalRevenue: Double,
    val totalOrders: Int,
    val activeListings: Int,
    val totalInventoryCount: Int,
    val topSellingProduct: String
)
