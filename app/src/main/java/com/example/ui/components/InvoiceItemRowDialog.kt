package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.InvoiceItem
import com.example.utils.PersianNumberHelper

@Composable
fun InvoiceItemRowDialog(
    initialItem: InvoiceItem? = null,
    currency: String = "تومان",
    onDismissRequest: () -> Unit,
    onConfirm: (InvoiceItem) -> Unit
) {
    var itemName by remember { mutableStateOf(initialItem?.itemName ?: "") }
    var itemCode by remember { mutableStateOf(initialItem?.itemCode ?: "") }
    var quantityText by remember {
        mutableStateOf(
            if (initialItem != null) {
                if (initialItem.quantity % 1.0 == 0.0) initialItem.quantity.toInt().toString()
                else initialItem.quantity.toString()
            } else "1"
        )
    }
    var unitPriceText by remember {
        mutableStateOf(
            if (initialItem != null && initialItem.unitPrice > 0) initialItem.unitPrice.toString() else ""
        )
    }

    var itemNameError by remember { mutableStateOf(false) }

    val qty = remember(quantityText) {
        PersianNumberHelper.parseDouble(quantityText).coerceAtLeast(0.0)
    }
    val unitPrice = remember(unitPriceText) {
        PersianNumberHelper.parseAmount(unitPriceText)
    }
    val rowTotal = remember(qty, unitPrice) {
        (qty * unitPrice).toLong()
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("invoice_item_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (initialItem == null) "افزودن ردیف کالا / خدمات" else "ویرایش ردیف کالا",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Item Name
                OutlinedTextField(
                    value = itemName,
                    onValueChange = {
                        itemName = it
                        if (it.isNotBlank()) itemNameError = false
                    },
                    label = { Text("نام یا شرح کالا / خدمات *") },
                    isError = itemNameError,
                    supportingText = if (itemNameError) {
                        { Text("لطفاً نام کالا را وارد کنید", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Item Code
                OutlinedTextField(
                    value = itemCode,
                    onValueChange = { itemCode = it },
                    label = { Text("کد کالا (اختیاری)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_code_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quantity & Stepper
                Text(
                    text = "تعداد / مقدار:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = {
                            val current = PersianNumberHelper.parseDouble(quantityText)
                            if (current > 1) {
                                val next = (current - 1).coerceAtLeast(1.0)
                                quantityText = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "کاهش")
                    }

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_quantity_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    FilledIconButton(
                        onClick = {
                            val current = PersianNumberHelper.parseDouble(quantityText)
                            val next = current + 1
                            quantityText = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "افزایش", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Unit Price (فی)
                OutlinedTextField(
                    value = unitPriceText,
                    onValueChange = { unitPriceText = PersianNumberHelper.toEnglishDigits(it) },
                    label = { Text("مبلغ واحد / فی ($currency) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        if (unitPrice > 0) {
                            Text(
                                text = PersianNumberHelper.formatPrice(unitPrice, currency),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_unit_price_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Live Row Calculation Box
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مبلغ خالص ردیف:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = PersianNumberHelper.formatPrice(rowTotal, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("انصراف")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (itemName.isBlank()) {
                                itemNameError = true
                                return@Button
                            }
                            val item = (initialItem ?: InvoiceItem(itemName = "")).copy(
                                itemName = itemName.trim(),
                                itemCode = itemCode.trim(),
                                quantity = if (qty <= 0.0) 1.0 else qty,
                                unitPrice = unitPrice,
                                totalPrice = rowTotal
                            )
                            onConfirm(item)
                            onDismissRequest()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_item_button")
                    ) {
                        Text(if (initialItem == null) "افزودن" else "بروزرسانی")
                    }
                }
            }
        }
    }
}
