package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val email: String,
    val password: String,
    val fullName: String,
    val storeName: String = "Apex Studio Store",
    val telegramUsername: String = "apex_support",
    val phoneNumber: String = "+1 (555) 234-5678",
    val bio: String = "Official verified merchant specializing in high-grade electronics and modern lifestyle products.",
    val isLoggedIn: Boolean = true,
    val role: String = "BUYER", // "ADMIN" or "BUYER"
    val shippingAddress: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isAdmin: Boolean
        get() = role.equals("ADMIN", ignoreCase = true) ||
                username.equals("Admin175", ignoreCase = true) ||
                email.equals("prasith1980@gmail.com", ignoreCase = true)

    val isBuyer: Boolean
        get() = !isAdmin
}

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Long = 1,
    val storeName: String = "Apex Studio Store",
    val storeTagline: String = "Premium curated electronics & modern lifestyle essentials",
    val telegramUsername: String = "apex_support",
    val supportEmail: String = "support@apexstore.com",
    val supportPhone: String = "+1 (555) 234-5678",
    val currencySymbol: String = "$",
    val taxRate: Double = 8.0,
    val orderNotificationEnabled: Boolean = true,
    val autoAcceptOrders: Boolean = true,
    val allowGuestCheckout: Boolean = true
)
