package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ProductEntity
import com.example.ui.util.ImageStorageHelper

val CATEGORIES = listOf("Electronics", "Fashion", "Home & Kitchen", "Lifestyle", "Accessories")
val IMAGE_OPTIONS = listOf(
    "img_product_headphone" to "Headphones",
    "img_product_watch" to "Smartwatch",
    "img_product_sneakers" to "Sneakers",
    "img_hero_banner" to "Showcase Banner",
    "ic_custom" to "Category Icon"
)

@Composable
fun AddEditProductDialog(
    productToEdit: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        price: Double,
        originalPrice: Double?,
        category: String,
        stockQuantity: Int,
        imageResName: String,
        sku: String,
        isFeatured: Boolean
    ) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(productToEdit?.title ?: "") }
    var description by remember { mutableStateOf(productToEdit?.description ?: "") }
    var priceText by remember { mutableStateOf(productToEdit?.price?.toString() ?: "") }
    var originalPriceText by remember { mutableStateOf(productToEdit?.originalPrice?.toString() ?: "") }
    var category by remember { mutableStateOf(productToEdit?.category ?: CATEGORIES.first()) }
    var stockText by remember { mutableStateOf(productToEdit?.stockQuantity?.toString() ?: "10") }
    var selectedImage by remember { mutableStateOf(productToEdit?.imageResName ?: "img_product_headphone") }
    var sku by remember { mutableStateOf(productToEdit?.sku ?: "") }
    var isFeatured by remember { mutableStateOf(productToEdit?.isFeatured ?: false) }

    var customUrlInput by remember {
        mutableStateOf(if (ImageStorageHelper.isWebUrl(selectedImage)) selectedImage else "")
    }
    var imageTabMode by remember {
        mutableIntStateOf(
            when {
                ImageStorageHelper.isFilePath(selectedImage) -> 0
                ImageStorageHelper.isWebUrl(selectedImage) -> 1
                else -> 2
            }
        )
    }
    var showImageOptions by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }

    // Photo Picker launcher for picking images from device gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageHelper.saveUriToInternalStorage(context, uri)
            if (savedPath != null) {
                selectedImage = savedPath
                uploadStatusMessage = "Photo uploaded successfully from device!"
                errorMessage = null
            } else {
                errorMessage = "Failed to copy image to app storage. Please try again."
            }
        }
    }

    // Fallback file picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageHelper.saveUriToInternalStorage(context, uri)
            if (savedPath != null) {
                selectedImage = savedPath
                uploadStatusMessage = "Photo selected from device storage!"
                errorMessage = null
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("add_edit_product_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (productToEdit != null) Icons.Default.Edit else Icons.Default.AddBusiness,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (productToEdit != null) "Edit Product Listing" else "List Product For Sale",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_add_edit_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (uploadStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uploadStatusMessage!!,
                                color = Color(0xFF2E7D32),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- PRODUCT IMAGE UPLOAD & PREVIEW SECTION ---
                Text(
                    text = "Product Image",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Image Preview Box with overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            ProductImage(
                                imageResName = selectedImage,
                                category = category,
                                contentDescription = "Product Image Preview",
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Gradient Scrim for readable badges
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                        )
                                    )
                            )

                            // Source Badge on Top Left
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            ImageStorageHelper.isFilePath(selectedImage) -> Icons.Default.PhotoLibrary
                                            ImageStorageHelper.isWebUrl(selectedImage) -> Icons.Default.Link
                                            else -> Icons.Default.Image
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            ImageStorageHelper.isFilePath(selectedImage) -> "Custom Upload"
                                            ImageStorageHelper.isWebUrl(selectedImage) -> "Web URL"
                                            else -> "Stock Preset"
                                        },
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Quick Change overlay button on Bottom Right
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clickable {
                                        try {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        } catch (e: Exception) {
                                            filePickerLauncher.launch("image/*")
                                        }
                                    }
                                    .testTag("upload_image_preview_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Upload Image",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Upload / Change",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Upload options tabs
                        TabRow(
                            selectedTabIndex = imageTabMode,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            divider = {}
                        ) {
                            Tab(
                                selected = imageTabMode == 0,
                                onClick = { imageTabMode = 0 },
                                text = { Text("Upload File", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Tab(
                                selected = imageTabMode == 1,
                                onClick = { imageTabMode = 1 },
                                text = { Text("Web URL", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Tab(
                                selected = imageTabMode == 2,
                                onClick = { imageTabMode = 2 },
                                text = { Text("Presets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tab 0: Upload File from Device
                        if (imageTabMode == 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            try {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            } catch (e: Exception) {
                                                filePickerLauncher.launch("image/*")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("choose_device_photo_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Choose from Device", fontSize = 12.sp)
                                    }

                                    if (ImageStorageHelper.isFilePath(selectedImage)) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedImage = "img_product_headphone"
                                                uploadStatusMessage = null
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Reset",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reset", fontSize = 12.sp)
                                        }
                                    }
                                }
                                Text(
                                    text = "Supported: JPG, PNG, WEBP from your gallery or camera",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Tab 1: Web URL
                        if (imageTabMode == 1) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customUrlInput,
                                        onValueChange = { customUrlInput = it },
                                        label = { Text("Direct Image URL") },
                                        placeholder = { Text("https://example.com/product.jpg") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_product_image_url"),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            val trimmed = customUrlInput.trim()
                                            if (ImageStorageHelper.isWebUrl(trimmed)) {
                                                selectedImage = trimmed
                                                uploadStatusMessage = "Web image URL applied!"
                                                errorMessage = null
                                            } else {
                                                errorMessage = "Please enter a valid URL starting with http:// or https://"
                                            }
                                        },
                                        enabled = customUrlInput.isNotBlank(),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("apply_image_url_button")
                                    ) {
                                        Text("Apply", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Tab 2: Stock Presets
                        if (imageTabMode == 2) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IMAGE_OPTIONS.take(3).forEach { (resKey, label) ->
                                        FilterChip(
                                            selected = selectedImage == resKey,
                                            onClick = {
                                                selectedImage = resKey
                                                uploadStatusMessage = null
                                            },
                                            label = { Text(label, fontSize = 11.sp) },
                                            modifier = Modifier.testTag("preset_image_$resKey")
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IMAGE_OPTIONS.drop(3).forEach { (resKey, label) ->
                                        FilterChip(
                                            selected = selectedImage == resKey,
                                            onClick = {
                                                selectedImage = resKey
                                                uploadStatusMessage = null
                                            },
                                            label = { Text(label, fontSize = 11.sp) },
                                            modifier = Modifier.testTag("preset_image_$resKey")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Product Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("Product Title *") },
                    placeholder = { Text("e.g. Wireless Pro Earbuds") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_title"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips
                Text(
                    text = "Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CATEGORIES.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CATEGORIES.drop(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Price and Original Price row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Selling Price ($) *") },
                        placeholder = { Text("49.99") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_product_price"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = originalPriceText,
                        onValueChange = { originalPriceText = it },
                        label = { Text("MSRP / Original ($)") },
                        placeholder = { Text("69.99") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_product_original_price"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stock and SKU row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Initial Stock *") },
                        placeholder = { Text("25") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_product_stock"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU Code") },
                        placeholder = { Text("SKU-1024") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_product_sku"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Product Description") },
                    placeholder = { Text("Highlight key specifications, materials, warranty, and features...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_description"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Featured Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Featured Spotlight",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Promote this product at the top of the store",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isFeatured,
                        onCheckedChange = { isFeatured = it },
                        modifier = Modifier.testTag("toggle_product_featured")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val price = priceText.toDoubleOrNull()
                            val originalPrice = originalPriceText.toDoubleOrNull()
                            val stock = stockText.toIntOrNull()

                            if (title.isBlank()) {
                                errorMessage = "Please enter a product title."
                            } else if (price == null || price <= 0) {
                                errorMessage = "Please enter a valid price greater than $0."
                            } else if (stock == null || stock < 0) {
                                errorMessage = "Please enter a valid stock quantity."
                            } else {
                                onSave(
                                    title.trim(),
                                    description.trim(),
                                    price,
                                    originalPrice,
                                    category,
                                    stock,
                                    selectedImage,
                                    sku.trim(),
                                    isFeatured
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_product_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (productToEdit != null) "Update Listing" else "List for Sale",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
