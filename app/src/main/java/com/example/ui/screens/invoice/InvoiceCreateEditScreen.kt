package com.example.ui.screens.invoice

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.BankAccount
import com.example.model.Customer
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceType
import com.example.ui.components.BankAccountSelectionDialog
import com.example.ui.components.CustomerSelectionDialog
import com.example.ui.components.InvoiceItemRowDialog
import com.example.ui.components.PersianDatePickerDialog
import com.example.ui.screens.bank.BankAccountEditDialog
import com.example.ui.screens.customer.CustomerEditDialog
import com.example.ui.viewmodel.BankAccountViewModel
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.InvoiceViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.utils.PersianDateHelper
import com.example.utils.PersianNumberHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceCreateEditScreen(
    invoiceViewModel: InvoiceViewModel,
    customerViewModel: CustomerViewModel,
    bankAccountViewModel: BankAccountViewModel = viewModel(),
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val invoice by invoiceViewModel.currentInvoice.collectAsStateWithLifecycle()
    val items by invoiceViewModel.currentItems.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val customers by customerViewModel.customersList.collectAsStateWithLifecycle()
    val bankAccounts by bankAccountViewModel.bankAccountsList.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showNewCustomerDialog by remember { mutableStateOf(false) }
    var showBankAccountPicker by remember { mutableStateOf(false) }
    var showNewBankAccountDialog by remember { mutableStateOf(false) }
    var showItemDialog by remember { mutableStateOf(false) }
    var itemToEditIndex by remember { mutableIntStateOf(-1) }
    var itemToEdit by remember { mutableStateOf<InvoiceItem?>(null) }

    var sellerSectionExpanded by remember { mutableStateOf(false) }
    var bankSectionExpanded by remember { mutableStateOf(true) }
    var vatInputText by remember { mutableStateOf(if (invoice.vatPercent > 0) invoice.vatPercent.toString() else "") }
    var discountInputText by remember { mutableStateOf(if (invoice.discountPercent > 0) invoice.discountPercent.toString() else "") }

    var customerNameError by remember { mutableStateOf(false) }

    Scaffold(
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
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (invoice.id == 0L) "صدور فاکتور جدید" else "ویرایش فاکتور #${PersianNumberHelper.toPersianDigits(invoice.invoiceNumber)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
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
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Invoice Type Segmented Control (فاکتور فروش vs پیش‌فاکتور)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "نوع فاکتور:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isProforma = invoice.invoiceType == InvoiceType.PROFORMA.name
                            SegmentedButton(
                                selected = isProforma,
                                onClick = {
                                    invoiceViewModel.updateInvoiceState { it.copy(invoiceType = InvoiceType.PROFORMA.name) }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("پیش‌فاکتور")
                            }
                            SegmentedButton(
                                selected = !isProforma,
                                onClick = {
                                    invoiceViewModel.updateInvoiceState { it.copy(invoiceType = InvoiceType.SALES.name) }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("فاکتور فروش")
                            }
                        }
                    }
                }
            }

            // 2. Invoice Meta Card (شماره فاکتور، تاریخ شمسی، واحد پول)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "اطلاعات پایه فاکتور",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Invoice Number
                            OutlinedTextField(
                                value = invoice.invoiceNumber,
                                onValueChange = { newNum ->
                                    invoiceViewModel.updateInvoiceState { it.copy(invoiceNumber = newNum) }
                                },
                                label = { Text("شماره فاکتور *") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("invoice_number_input"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Date Field (Clickable for Shamsi DatePicker)
                            OutlinedTextField(
                                value = PersianNumberHelper.toPersianDigits(invoice.issueDateShamsi),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("تاریخ (شمسی)") },
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = "انتخاب تاریخ")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showDatePicker = true }
                                    .testTag("invoice_date_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Currency Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "واحد پول فاکتور:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = invoice.currency == "تومان",
                                    onClick = { invoiceViewModel.updateInvoiceState { it.copy(currency = "تومان") } },
                                    label = { Text("تومان") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                FilterChip(
                                    selected = invoice.currency == "ریال",
                                    onClick = { invoiceViewModel.updateInvoiceState { it.copy(currency = "ریال") } },
                                    label = { Text("ریال") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Customer / Buyer Details Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "مشخصات صورتحساب (خریدار)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FilledTonalButton(
                                onClick = { showCustomerPicker = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("انتخاب مشتری", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Customer Name
                        OutlinedTextField(
                            value = invoice.customerName,
                            onValueChange = {
                                invoiceViewModel.updateInvoiceState { inv -> inv.copy(customerName = it) }
                                if (it.isNotBlank()) customerNameError = false
                            },
                            label = { Text("نام و نام‌خانوادگی خریدار *") },
                            isError = customerNameError,
                            supportingText = if (customerNameError) {
                                { Text("وارد کردن نام خریدار الزامی است", color = MaterialTheme.colorScheme.error) }
                            } else null,
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_customer_name_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Customer Phone
                        OutlinedTextField(
                            value = invoice.customerPhone,
                            onValueChange = {
                                val clean = PersianNumberHelper.toEnglishDigits(it)
                                invoiceViewModel.updateInvoiceState { inv -> inv.copy(customerPhone = clean) }
                            },
                            label = { Text("شماره تماس خریدار") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_customer_phone_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Customer Address
                        OutlinedTextField(
                            value = invoice.customerAddress,
                            onValueChange = {
                                invoiceViewModel.updateInvoiceState { inv -> inv.copy(customerAddress = it) }
                            },
                            label = { Text("نشانی و آدرس خریدار") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_customer_address_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // 4. Seller Details (Collapsible Card)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sellerSectionExpanded = !sellerSectionExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "مشخصات فروشنده / کسب‌وکار",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                if (sellerSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(visible = sellerSectionExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                OutlinedTextField(
                                    value = invoice.sellerName,
                                    onValueChange = { invoiceViewModel.updateInvoiceState { inv -> inv.copy(sellerName = it) } },
                                    label = { Text("نام فروشگاه / فروشنده") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = invoice.sellerPhone,
                                    onValueChange = { invoiceViewModel.updateInvoiceState { inv -> inv.copy(sellerPhone = it) } },
                                    label = { Text("تلفن فروشنده") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = invoice.sellerAddress,
                                    onValueChange = { invoiceViewModel.updateInvoiceState { inv -> inv.copy(sellerAddress = it) } },
                                    label = { Text("نشانی فروشنده") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = invoice.sellerEconomicCode,
                                    onValueChange = { invoiceViewModel.updateInvoiceState { inv -> inv.copy(sellerEconomicCode = it) } },
                                    label = { Text("کد اقتصادی / شماره ثبت") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Products / Services Body (جدول ردیف‌های کالا)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "اقلام و خدمات فاکتور",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${PersianNumberHelper.toPersianDigits(items.size.toString())} ردیف ثبت شده",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    itemToEdit = null
                                    itemToEditIndex = -1
                                    showItemDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_item_row_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("افزودن کالا")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (items.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "هیچ کالایی به فاکتور اضافه نشده است",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Table items
                            items.forEachIndexed { index, item ->
                                InvoiceItemCardRow(
                                    index = index,
                                    item = item,
                                    currency = invoice.currency,
                                    onEdit = {
                                        itemToEdit = item
                                        itemToEditIndex = index
                                        showItemDialog = true
                                    },
                                    onDelete = {
                                        invoiceViewModel.removeItem(index)
                                    }
                                )
                                if (index < items.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Financial Calculations / Summary (محاسبات، تخفیف، ارزش افزوده، تبدیل به حروف)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "محاسبات و جمع‌بندی مالی",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Subtotal Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "جمع کل اقلام:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = PersianNumberHelper.formatPrice(invoice.subtotal, invoice.currency),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // VAT (ارزش افزوده)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = vatInputText,
                                onValueChange = {
                                    vatInputText = PersianNumberHelper.toEnglishDigits(it)
                                    val pct = PersianNumberHelper.parseDouble(vatInputText)
                                    invoiceViewModel.updateVat(pct)
                                },
                                label = { Text("درصد مالیات ارزش افزوده (%)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("vat_percent_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Text(
                                text = "+ ${PersianNumberHelper.formatPrice(invoice.vatAmount, invoice.currency)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Discount (تخفیف)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = discountInputText,
                                onValueChange = {
                                    discountInputText = PersianNumberHelper.toEnglishDigits(it)
                                    val pct = PersianNumberHelper.parseDouble(discountInputText)
                                    invoiceViewModel.updateDiscount(pct)
                                },
                                label = { Text("درصد تخفیف (%)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("discount_percent_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Text(
                                text = "- ${PersianNumberHelper.formatPrice(invoice.discountAmount, invoice.currency)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Final Net Amount Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "مبلغ خالص نهایی (قابل پرداخت):",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = PersianNumberHelper.formatPrice(invoice.finalTotal, invoice.currency),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Number to Persian Words
                                Text(
                                    text = "به حروف: " + PersianNumberHelper.numberToPersianWords(invoice.finalTotal, invoice.currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // 7. Settlement Bank Account Card (اطلاعات حساب بانکی جهت تسویه)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اطلاعات حساب بانکی جهت تسویه",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalButton(
                                    onClick = { showBankAccountPicker = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("select_bank_account_button")
                                ) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("انتخاب حساب", style = MaterialTheme.typography.labelSmall)
                                }

                                if (invoice.bankName.isNotBlank() || invoice.bankCardNumber.isNotBlank() || invoice.bankIban.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            invoiceViewModel.updateInvoiceState { inv ->
                                                inv.copy(
                                                    bankAccountId = null,
                                                    bankName = "",
                                                    bankBranch = "",
                                                    bankAccountNumber = "",
                                                    bankIban = "",
                                                    bankCardNumber = "",
                                                    bankAccountHolder = "",
                                                    bankDepositNotes = ""
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "پاک کردن حساب",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bank Name & Branch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = invoice.bankName,
                                onValueChange = { invoiceViewModel.updateInvoiceState { inv -> inv.copy(bankName = it) } },
                                label = { Text("نام بانک (مثال: ملی، ملت)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("invoice_bank_name_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = invoice.bankBranch,
                                onValueChange = { invoiceViewModel.updateInvoiceState { inv -> inv.copy(bankBranch = it) } },
                                label = { Text("شعبه") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("invoice_bank_branch_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Account Holder Name
                        OutlinedTextField(
                            value = invoice.bankAccountHolder,
                            onValueChange = { invoiceViewModel.updateInvoiceState { inv -> inv.copy(bankAccountHolder = it) } },
                            label = { Text("نام صاحب حساب") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_bank_holder_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Card Number (16 Digits)
                        OutlinedTextField(
                            value = invoice.bankCardNumber,
                            onValueChange = {
                                invoiceViewModel.updateInvoiceState { inv ->
                                    inv.copy(bankCardNumber = PersianNumberHelper.toEnglishDigits(it.replace("-", "").replace(" ", "")))
                                }
                            },
                            label = { Text("شماره کارت (۱۶ رقم)") },
                            placeholder = { Text("---- ---- ---- ----") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_bank_card_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // IBAN & Account Number
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = invoice.bankIban,
                                onValueChange = {
                                    val clean = PersianNumberHelper.toEnglishDigits(it)
                                        .uppercase()
                                        .filter { char -> char.isLetterOrDigit() }
                                        .take(26)
                                    invoiceViewModel.updateInvoiceState { inv -> inv.copy(bankIban = clean) }
                                },
                                label = { Text("شماره شبا (IBAN)") },
                                placeholder = { Text("IR--") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("invoice_bank_iban_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = invoice.bankAccountNumber,
                                onValueChange = {
                                    invoiceViewModel.updateInvoiceState { inv ->
                                        inv.copy(bankAccountNumber = PersianNumberHelper.toEnglishDigits(it))
                                    }
                                },
                                label = { Text("شماره حساب") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("invoice_bank_account_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Deposit Notes
                        OutlinedTextField(
                            value = invoice.bankDepositNotes,
                            onValueChange = { invoiceViewModel.updateInvoiceState { inv -> inv.copy(bankDepositNotes = it) } },
                            label = { Text("توضیحات واریز (شناسه واریز یا شرایط پرداخت)") },
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_bank_deposit_notes_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // 8. Footer Notes & Signatures Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "توضیحات و امضاء فاکتور",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = invoice.notes,
                            onValueChange = { invoiceViewModel.updateInvoiceState { inv -> inv.copy(notes = it) } },
                            label = { Text("توضیحات و شرایط فاکتور") },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invoice_notes_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual signature boxes preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "مهر و امضاء خریدار",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "مهر و امضاء فروشنده",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 9. Bottom Action Buttons: Save, Share, Preview PDF
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary Save Button
                    Button(
                        onClick = {
                            if (invoice.customerName.isBlank()) {
                                customerNameError = true
                                Toast.makeText(context, "لطفاً نام خریدار را وارد کنید", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            invoiceViewModel.saveCurrentInvoice {
                                Toast.makeText(context, "فاکتور با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_invoice_bottom_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ذخیره فاکتور",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    // Secondary Row: Preview PDF & Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (invoice.customerName.isBlank()) {
                                    Toast.makeText(context, "لطفاً ابتدا نام خریدار را وارد کنید", Toast.LENGTH_SHORT).show()
                                    return@FilledTonalButton
                                }
                                invoiceViewModel.generatePdfForCurrentInvoice(context, settings) { pdfFile ->
                                    invoiceViewModel.generateAndViewPdf(
                                        context,
                                        com.example.model.InvoiceWithItems(invoice, items),
                                        settings
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("پیش‌نمایش PDF")
                        }

                        FilledTonalButton(
                            onClick = {
                                if (invoice.customerName.isBlank()) {
                                    Toast.makeText(context, "لطفاً ابتدا نام خریدار را وارد کنید", Toast.LENGTH_SHORT).show()
                                    return@FilledTonalButton
                                }
                                invoiceViewModel.generatePdfForCurrentInvoice(context, settings) { pdfFile ->
                                    invoiceViewModel.generateAndSharePdf(
                                        context,
                                        com.example.model.InvoiceWithItems(invoice, items),
                                        settings
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اشتراک‌گذاری")
                        }
                    }
                }
            }
        }
    }

    // Shamsi Date Picker Dialog
    if (showDatePicker) {
        PersianDatePickerDialog(
            initialDate = invoice.issueDateShamsi,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                invoiceViewModel.updateInvoiceState { it.copy(issueDateShamsi = selectedDate) }
            }
        )
    }

    // Customer Selection Dialog
    if (showCustomerPicker) {
        CustomerSelectionDialog(
            customers = customers,
            onDismissRequest = { showCustomerPicker = false },
            onCustomerSelected = { selectedCustomer ->
                invoiceViewModel.updateInvoiceState { inv ->
                    inv.copy(
                        customerId = selectedCustomer.id,
                        customerName = selectedCustomer.name,
                        customerPhone = selectedCustomer.phone,
                        customerAddress = selectedCustomer.address
                    )
                }
            },
            onAddNewCustomer = {
                showNewCustomerDialog = true
            }
        )
    }

    // Add New Customer directly from invoice dialog
    if (showNewCustomerDialog) {
        CustomerEditDialog(
            onDismissRequest = { showNewCustomerDialog = false },
            onConfirm = { newCustomer ->
                customerViewModel.saveCustomer(newCustomer) { newId ->
                    invoiceViewModel.updateInvoiceState { inv ->
                        inv.copy(
                            customerId = newId,
                            customerName = newCustomer.name,
                            customerPhone = newCustomer.phone,
                            customerAddress = newCustomer.address
                        )
                    }
                }
            }
        )
    }

    // Bank Account Selection Dialog
    if (showBankAccountPicker) {
        BankAccountSelectionDialog(
            bankAccounts = bankAccounts,
            onDismissRequest = { showBankAccountPicker = false },
            onAccountSelected = { selectedAccount ->
                invoiceViewModel.updateInvoiceState { inv ->
                    inv.copy(
                        bankAccountId = selectedAccount.id,
                        bankName = selectedAccount.bankName,
                        bankBranch = selectedAccount.branchName,
                        bankAccountNumber = selectedAccount.accountNumber,
                        bankIban = selectedAccount.iban,
                        bankCardNumber = selectedAccount.cardNumber,
                        bankAccountHolder = selectedAccount.accountHolder,
                        bankDepositNotes = selectedAccount.depositNotes
                    )
                }
            },
            onAddNewAccount = {
                showNewBankAccountDialog = true
            }
        )
    }

    // Add New Bank Account directly from invoice dialog
    if (showNewBankAccountDialog) {
        BankAccountEditDialog(
            onDismissRequest = { showNewBankAccountDialog = false },
            onConfirm = { newAccount ->
                bankAccountViewModel.saveBankAccount(newAccount) { newId ->
                    invoiceViewModel.updateInvoiceState { inv ->
                        inv.copy(
                            bankAccountId = newId,
                            bankName = newAccount.bankName,
                            bankBranch = newAccount.branchName,
                            bankAccountNumber = newAccount.accountNumber,
                            bankIban = newAccount.iban,
                            bankCardNumber = newAccount.cardNumber,
                            bankAccountHolder = newAccount.accountHolder,
                            bankDepositNotes = newAccount.depositNotes
                        )
                    }
                }
            }
        )
    }

    // Add/Edit Item Row Dialog
    if (showItemDialog) {
        InvoiceItemRowDialog(
            initialItem = itemToEdit,
            currency = invoice.currency,
            onDismissRequest = {
                showItemDialog = false
                itemToEdit = null
                itemToEditIndex = -1
            },
            onConfirm = { savedItem ->
                if (itemToEditIndex >= 0) {
                    invoiceViewModel.updateItem(itemToEditIndex, savedItem)
                } else {
                    invoiceViewModel.addItem(savedItem)
                }
            }
        )
    }
}

@Composable
fun InvoiceItemCardRow(
    index: Int,
    item: InvoiceItem,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Row number badge
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = PersianNumberHelper.toPersianDigits((index + 1).toString()),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Name & Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            val codeStr = if (item.itemCode.isNotBlank()) " | کد: ${PersianNumberHelper.toPersianDigits(item.itemCode)}" else ""
            Text(
                text = "${PersianNumberHelper.toPersianDigits(qtyStr)} عدد × ${PersianNumberHelper.formatPrice(item.unitPrice, currency)}$codeStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Total price
        Text(
            text = PersianNumberHelper.formatPrice(item.totalPrice, currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "حذف ردیف",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
