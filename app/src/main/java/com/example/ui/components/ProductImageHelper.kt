package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.util.ImageStorageHelper
import java.io.File

@Composable
fun ProductImage(
    imageResName: String,
    category: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current

    // Case 1: Web URL or Device File Path
    if (ImageStorageHelper.isWebUrl(imageResName) || ImageStorageHelper.isFilePath(imageResName)) {
        val imageModel: Any = if (imageResName.startsWith("/")) {
            File(imageResName)
        } else {
            imageResName
        }

        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageModel)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            error = {
                FallbackCategoryPlaceholder(
                    category = category,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
        return
    }

    // Case 2: Bundled Drawable Resource Name
    val drawableId = when (imageResName) {
        "img_product_headphone" -> R.drawable.img_product_headphone
        "img_product_watch" -> R.drawable.img_product_watch
        "img_product_sneakers" -> R.drawable.img_product_sneakers
        "img_hero_banner" -> R.drawable.img_hero_banner
        "img_app_icon" -> R.drawable.img_app_icon
        else -> {
            val resId = context.resources.getIdentifier(imageResName, "drawable", context.packageName)
            if (resId != 0) resId else null
        }
    }

    if (drawableId != null) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        FallbackCategoryPlaceholder(
            category = category,
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}

@Composable
fun FallbackCategoryPlaceholder(
    category: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val icon = when (category.lowercase()) {
        "electronics" -> Icons.Default.Devices
        "fashion", "apparel" -> Icons.Default.ShoppingBag
        "home & kitchen", "home" -> Icons.Default.Home
        "beauty", "lifestyle" -> Icons.Default.Spa
        else -> Icons.Default.Inventory2
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
