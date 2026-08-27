package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

object TelegramHelper {
    fun openTelegramChat(
        context: Context,
        telegramUsername: String,
        productTitle: String? = null,
        productPrice: Double? = null,
        sku: String? = null
    ) {
        val cleanUsername = telegramUsername.trim().removePrefix("@").ifBlank { "apex_support" }

        val message = buildString {
            if (!productTitle.isNullOrBlank()) {
                append("Hello! I'm interested in: ")
                append(productTitle)
                if (productPrice != null) {
                    append(String.format(Locale.US, " ($%.2f)", productPrice))
                }
                if (!sku.isNullOrBlank()) {
                    append(" [SKU: $sku]")
                }
                append(". Is this item available?")
            } else {
                append("Hello! I would like to inquire about your store products.")
            }
        }

        val encodedMessage = try {
            URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            message.replace(" ", "%20")
        }

        val tgAppUri = Uri.parse("tg://resolve?domain=$cleanUsername&text=$encodedMessage")
        val webUri = Uri.parse("https://t.me/$cleanUsername?text=$encodedMessage")

        val appIntent = Intent(Intent.ACTION_VIEW, tgAppUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(appIntent)
        } catch (e: Exception) {
            try {
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                Toast.makeText(
                    context,
                    "Opening Telegram: @$cleanUsername",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
