package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.BusinessSettings
import com.example.ui.viewmodel.SettingsViewModel
import com.example.utils.PersianDateHelper
import com.example.utils.PersianNumberHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isSettingsLoaded by viewModel.isSettingsLoaded.collectAsStateWithLifecycle()

    var isInitialized by remember { mutableStateOf(false) }

    var businessName by remember { mutableStateOf("") }
    var businessPhone by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var economicCode by remember { mutableStateOf("") }
    var defaultFooterNote by remember { mutableStateOf("") }
    val currency = settings.currency
    val appTheme = settings.appTheme
    val pdfTheme = settings.pdfTheme

    // Initial state loading from database (triggered only once after DB emission or on manual reset)
    LaunchedEffect(settings, isSettingsLoaded, isInitialized) {
        if (isSettingsLoaded && !isInitialized) {
            businessName = settings.businessName
            businessPhone = settings.businessPhone
            businessAddress = settings.businessAddress
            economicCode = settings.economicCode
            defaultFooterNote = settings.defaultFooterNote
            isInitialized = true
        }
    }

    // Debounced autosave: waits 400ms after user pauses typing/backspacing before persisting to DB
    LaunchedEffect(businessName, businessPhone, businessAddress, economicCode, defaultFooterNote, isInitialized) {
        if (isInitialized) {
            delay(400)
            val current = viewModel.settings.value
            if (current.businessName != businessName ||
                current.businessPhone != businessPhone ||
                current.businessAddress != businessAddress ||
                current.economicCode != economicCode ||
                current.defaultFooterNote != defaultFooterNote
            ) {
                viewModel.updateSettings(
                    current.copy(
                        businessName = businessName,
                        businessPhone = businessPhone,
                        businessAddress = businessAddress,
                        economicCode = economicCode,
                        defaultFooterNote = defaultFooterNote
                    )
                )
            }
        }
    }

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var intervalDropdownExpanded by remember { mutableStateOf(false) }

    // Logo image picker launcher with persistent private storage copy
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveLogoFromUri(context, uri)
            Toast.makeText(context, "لوگوی جدید با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
        }
    }

    // Seller stamp & signature picker launcher
    val signaturePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveSignatureFromUri(context, uri)
            Toast.makeText(context, "مُهر و امضای فروشنده با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
        }
    }

    // JSON file restore launcher
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedRestoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

    // SAF directory picker for auto-backup
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}
            viewModel.updateBackupFolderUri(uri.toString())
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "تنظیمات کسب‌وکار و پشتیبان‌گیری",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ذخیره خودکار",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Business Info & Logo Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مشخصات کسب‌وکار / فروشگاه",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Logo upload/preview box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clickable { logoPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!settings.logoUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = settings.logoUri,
                                        contentDescription = "لوگوی کسب‌وکار",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "لوگوی رسمی کسب‌وکار",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "لوگو در سربرگ فاکتورهای خروجی چاپ می‌شود.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = { logoPickerLauncher.launch("image/*") },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("انتخاب لوگو", style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (!settings.logoUri.isNullOrBlank()) {
                                        TextButton(onClick = { viewModel.updateLogoUri(null) }) {
                                            Text("حذف", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Seller Stamp & Signature Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clickable { signaturePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!settings.signatureUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = settings.signatureUri,
                                        contentDescription = "مُهر و امضای فروشنده",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.DriveFileRenameOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "مُهر و امضای فروشنده",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "در کادر مهر و امضای فروشنده در فاکتور PDF چاپ می‌شود.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = { signaturePickerLauncher.launch("image/*") },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("انتخاب مُهر / امضا", style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (!settings.signatureUri.isNullOrBlank()) {
                                        TextButton(onClick = { viewModel.updateSignatureUri(null) }) {
                                            Text("حذف", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Business Name
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = {
                                businessName = it
                            },
                            label = { Text("نام فروشگاه یا شرکت") },
                            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_business_name_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Business Phone
                        OutlinedTextField(
                            value = businessPhone,
                            onValueChange = {
                                businessPhone = PersianNumberHelper.toEnglishDigits(it)
                            },
                            label = { Text("شماره تماس فروشگاه") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_business_phone_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Business Address
                        OutlinedTextField(
                            value = businessAddress,
                            onValueChange = {
                                businessAddress = it
                            },
                            label = { Text("نشانی و آدرس فروشگاه") },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_business_address_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Economic Code
                        OutlinedTextField(
                            value = economicCode,
                            onValueChange = {
                                economicCode = it
                            },
                            label = { Text("کد اقتصادی / شماره ثبت / شناسه ملی") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_economic_code_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Default Footer Note
                        OutlinedTextField(
                            value = defaultFooterNote,
                            onValueChange = {
                                defaultFooterNote = it
                            },
                            label = { Text("توضیحات و شرایط پیش‌فرض ذیل فاکتور") },
                            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_footer_note_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Default Currency Toggle
                        Text(
                            text = "واحد پول پیش‌فرض سیستم:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = currency == "تومان",
                                onClick = {
                                    viewModel.updateCurrency("تومان")
                                },
                                label = { Text("تومان") },
                                shape = RoundedCornerShape(8.dp)
                            )
                            FilterChip(
                                selected = currency == "ریال",
                                onClick = {
                                    viewModel.updateCurrency("ریال")
                                },
                                label = { Text("ریال") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // 2. Appearance & PDF Themes Card (تم برنامه و تم رنگی PDF)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ظاهر برنامه و تم رنگی PDF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // A. App Theme (روشن / تیره / سیستم)
                        Text(
                            text = "حالت تم برنامه:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = appTheme == "SYSTEM",
                                onClick = {
                                    viewModel.updateAppTheme("SYSTEM")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.SettingsBrightness, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("سیستم") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = appTheme == "LIGHT",
                                onClick = {
                                    viewModel.updateAppTheme("LIGHT")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("روشن") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = appTheme == "DARK",
                                onClick = {
                                    viewModel.updateAppTheme("DARK")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("تیره") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // B. PDF Color Palette
                        Text(
                            text = "تم رنگی فاکتورهای PDF خروجی:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "رنگ سربرگ، جداول و بخش جمع کل فاکتورها مطابق رنگ انتخابی تغییر می‌کند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val pdfThemes = listOf(
                            Triple("SLATE", "خاکستری مدرن", Color(0xFF1E293B)),
                            Triple("CRIMSON", "قرمز رسمی", Color(0xFFDC2626)),
                            Triple("NAVY", "سرمه‌ای رسمی", Color(0xFF1E3A8A)),
                            Triple("EMERALD", "سبز زمردی", Color(0xFF065F46)),
                            Triple("PURPLE", "بنفش سلطنتی", Color(0xFF581C87)),
                            Triple("AMBER", "طلایی / کهربایی", Color(0xFF78350F))
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pdfThemes.chunked(2).forEach { rowThemes ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowThemes.forEach { (key, label, color) ->
                                        val isSelected = pdfTheme == key
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    viewModel.updatePdfTheme(key)
                                                }
                                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                )
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Manual Backup & Restore Section (پشتیبان‌گیری دستی)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "پشتیبان‌گیری و بازیابی اطلاعات (JSON)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "تمامی داده‌های مشتریان، فاکتورها، اقلام و تنظیمات در یک فایل JSON امن ذخیره می‌شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.exportManualBackup(context)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("پشتیبان‌گیری")
                            }

                            OutlinedButton(
                                onClick = {
                                    restoreFileLauncher.launch("application/json")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("بازیابی اطلاعات")
                            }
                        }
                    }
                }
            }

            // 3. Automated Background Backup (بکاپ خودکار با WorkManager)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "بکاپ خودکار در پس‌زمینه",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "مدیریت با WorkManager اندروید",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = settings.autoBackupEnabled,
                                onCheckedChange = { viewModel.toggleAutoBackup(it) },
                                modifier = Modifier.testTag("auto_backup_switch")
                            )
                        }

                        AnimatedVisibility(visible = settings.autoBackupEnabled) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Interval Selector
                                Text(
                                    text = "دوره تناوب پشتیبان‌گیری خودکار:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val intervals = listOf(
                                        "DAILY" to "روزانه",
                                        "WEEKLY" to "هفتگی",
                                        "MONTHLY" to "ماهانه"
                                    )
                                    intervals.forEach { (key, label) ->
                                        FilterChip(
                                            selected = settings.autoBackupInterval.equals(key, ignoreCase = true),
                                            onClick = { viewModel.updateAutoBackupInterval(key) },
                                            label = { Text(label) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // SAF Destination Folder Picker
                                Text(
                                    text = "مسیر ذخیره‌سازی فایل‌های بکاپ خودکار:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = if (!settings.backupFolderUri.isNullOrBlank()) "پوشه اختصاصی انتخاب شده" else "پوشه داخلی اپلیکیشن",
                                        onValueChange = {},
                                        readOnly = true,
                                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = { folderPickerLauncher.launch(null) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("انتخاب پوشه")
                                    }
                                }

                                if (settings.lastBackupTimestamp > 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val dateStr = PersianDateHelper.formatFromTimestamp(settings.lastBackupTimestamp)
                                        Text(
                                            text = "آخرین بکاپ موفق: ${PersianNumberHelper.toPersianDigits(dateStr)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Developer / Designer Credit
            item {
                val isDarkTheme = when (appTheme) {
                    "DARK" -> true
                    "LIGHT" -> false
                    else -> isSystemInDarkTheme()
                }
                val footerBoxColor = if (isDarkTheme) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                } else {
                    Color(0xFFE2E8F0).copy(alpha = 0.75f)
                }
                val footerBorderColor = if (isDarkTheme) {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
                } else {
                    Color(0xFFCBD5E1)
                }
                val footerTextColor = if (isDarkTheme) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                } else {
                    Color(0xFF334155)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Unified distinct container for footer credits
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = footerBoxColor,
                        border = BorderStroke(
                            width = 1.dp,
                            color = footerBorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Personal Website Link
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "طراحی و توسعه با ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = footerTextColor
                                )
                                Text(
                                    text = "«مهدی صیادناهد»",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mehdisayyadnahed.ir")).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "مرورگری برای باز کردن پیوند یافت نشد", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            // 2. Github Profile / Feedback Link
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "ارسال پیشنهاد بهبود و توسعه‌ی این نرم‌افزار در ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = footerTextColor
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mehdisayyadnahed/factor-hamrah")).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "مرورگری برای باز کردن پیوند یافت نشد", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Github",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = TextDecoration.Underline
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_github),
                                        contentDescription = "لوگوی گیت‌هاب",
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // 3. Tribute to Saber Rastikerdar for Vazirmatn Font
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "به یاد «صابر راستی‌کردار» برای خلق قلم زیبای ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = footerTextColor
                                )
                                Text(
                                    text = "وزیرمتن",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rastikerdar.github.io")).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "مرورگری برای باز کردن پیوند یافت نشد", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog && selectedRestoreUri != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                selectedRestoreUri = null
            },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("تأیید بازیابی اطلاعات") },
            text = {
                Text("با اجرای بازیابی، اطلاعات موجود با داده‌های فایل پشتیبان جایگزین خواهند شد. آیا مطمئن هستید؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedRestoreUri
                        showRestoreConfirmDialog = false
                        selectedRestoreUri = null
                        if (uri != null) {
                            viewModel.restoreBackup(context, uri) { success, message ->
                                if (success) {
                                    isInitialized = false
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("بله، بازیابی شود")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        selectedRestoreUri = null
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
}
