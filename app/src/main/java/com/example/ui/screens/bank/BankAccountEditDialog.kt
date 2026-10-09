package com.example.ui.screens.bank

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BankAccount
import com.example.utils.PersianNumberHelper

@Composable
fun BankAccountEditDialog(
    account: BankAccount? = null,
    onDismissRequest: () -> Unit,
    onConfirm: (BankAccount) -> Unit
) {
    var bankName by remember { mutableStateOf(account?.bankName ?: "") }
    var branchName by remember { mutableStateOf(account?.branchName ?: "") }
    var accountHolder by remember { mutableStateOf(account?.accountHolder ?: "") }
    var cardNumber by remember { mutableStateOf(account?.cardNumber ?: "") }
    var iban by remember { mutableStateOf(account?.iban ?: "") }
    var accountNumber by remember { mutableStateOf(account?.accountNumber ?: "") }
    var depositNotes by remember { mutableStateOf(account?.depositNotes ?: "") }
    var isDefault by remember { mutableStateOf(account?.isDefault ?: false) }

    var bankNameError by remember { mutableStateOf(false) }
    var accountHolderError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp)
                .testTag("bank_account_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (account == null) "ثبت اطلاعات حساب بانکی" else "ویرایش حساب بانکی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bank Name Field
                OutlinedTextField(
                    value = bankName,
                    onValueChange = {
                        bankName = it
                        if (it.isNotBlank()) bankNameError = false
                    },
                    label = { Text("نام بانک (مثال: ملی، ملت، صادرات) *") },
                    isError = bankNameError,
                    supportingText = if (bankNameError) {
                        { Text("وارد کردن نام بانک الزامی است", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Branch Name Field
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    label = { Text("نام یا کد شعبه") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_branch_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Account Holder Field
                OutlinedTextField(
                    value = accountHolder,
                    onValueChange = {
                        accountHolder = it
                        if (it.isNotBlank()) accountHolderError = false
                    },
                    label = { Text("نام صاحب حساب *") },
                    isError = accountHolderError,
                    supportingText = if (accountHolderError) {
                        { Text("وارد کردن نام صاحب حساب الزامی است", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_account_holder_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Card Number Field (16 Digits)
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = {
                        cardNumber = PersianNumberHelper.toEnglishDigits(it.replace("-", "").replace(" ", ""))
                    },
                    label = { Text("شماره کارت (۱۶ رقم)") },
                    placeholder = { Text("---- ---- ---- ----") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_card_number_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // IBAN Field (شماره شبا)
                OutlinedTextField(
                    value = iban,
                    onValueChange = {
                        iban = PersianNumberHelper.toEnglishDigits(it)
                            .uppercase()
                            .filter { char -> char.isLetterOrDigit() }
                            .take(26)
                    },
                    label = { Text("شماره شبا (IBAN)") },
                    placeholder = { Text("IR------------------------") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_iban_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Account Number Field
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = {
                        accountNumber = PersianNumberHelper.toEnglishDigits(it)
                    },
                    label = { Text("شماره حساب") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_account_number_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Deposit Notes Field
                OutlinedTextField(
                    value = depositNotes,
                    onValueChange = { depositNotes = it },
                    label = { Text("توضیحات واریز (شناسه واریز، مهلت و ...)") },
                    maxLines = 2,
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_deposit_notes_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Is Default Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        modifier = Modifier.testTag("bank_is_default_checkbox")
                    )
                    Text(
                        text = "انتخاب به عنوان حساب پیش‌فرض تسویه فاکتورها",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
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
                            if (bankName.isBlank()) {
                                bankNameError = true
                                return@Button
                            }
                            if (accountHolder.isBlank()) {
                                accountHolderError = true
                                return@Button
                            }
                            val cleanIban = iban.trim().uppercase()
                            val formattedIban = if (cleanIban.isNotBlank()) {
                                if (!cleanIban.startsWith("IR") && cleanIban.all { it.isDigit() }) "IR$cleanIban" else cleanIban
                            } else ""

                            val updatedAccount = (account ?: BankAccount()).copy(
                                bankName = bankName.trim(),
                                branchName = branchName.trim(),
                                accountHolder = accountHolder.trim(),
                                cardNumber = cardNumber.trim(),
                                iban = formattedIban,
                                accountNumber = accountNumber.trim(),
                                depositNotes = depositNotes.trim(),
                                isDefault = isDefault
                            )
                            onConfirm(updatedAccount)
                            onDismissRequest()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_bank_account_button")
                    ) {
                        Text(if (account == null) "ثبت حساب" else "ذخیره تغییرات")
                    }
                }
            }
        }
    }
}
