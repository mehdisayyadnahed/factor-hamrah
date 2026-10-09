package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.BusinessSettings
import com.example.model.InvoiceType
import com.example.model.InvoiceWithItems
import java.io.File
import java.io.FileOutputStream

object PdfInvoiceGenerator {

    // A4 dimensions in points (72 points per inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val OUTER_MARGIN = 20f
    private const val INNER_PADDING = 8f // Space between outer border and inner content boxes
    private const val CORNER_RADIUS = 6f // Uniform corner radius across all sections & tables

    data class PdfColorScheme(
        val primaryColor: Int,
        val secondaryColor: Int,
        val headerBgColor: Int,
        val tableHeaderColor: Int,
        val tableHeaderTextColor: Int,
        val alternatingRowColor: Int,
        val summaryBgColor: Int,
        val payableBgColor: Int,
        val payableTextColor: Int,
        val borderColor: Int,
        val lightBorderColor: Int,
        val textColor: Int,
        val textMutedColor: Int
    )

    private fun getPdfColorScheme(themeName: String, isProforma: Boolean): PdfColorScheme {
        val base = when (themeName.uppercase()) {
            "EMERALD" -> PdfColorScheme(
                primaryColor = Color.parseColor("#065F46"),         // Deep Emerald
                secondaryColor = Color.parseColor("#059669"),       // Vibrant Emerald
                headerBgColor = Color.parseColor("#ECFDF5"),        // Light soft mint/emerald tint
                tableHeaderColor = Color.parseColor("#047857"),     // Solid Emerald table header
                tableHeaderTextColor = Color.WHITE,                // Crisp white text on dark header
                alternatingRowColor = Color.parseColor("#F0FDF4"),  // Subtle alternating row tint
                summaryBgColor = Color.parseColor("#F0FDF4"),       // Light mint
                payableBgColor = Color.parseColor("#A7F3D0"),       // Distinct fresh emerald container
                payableTextColor = Color.parseColor("#064E3B"),     // High contrast dark emerald text
                borderColor = Color.parseColor("#34D399"),          // Theme border
                lightBorderColor = Color.parseColor("#D1FAE5"),     // Light divider border
                textColor = Color.parseColor("#0F172A"),            // Deep slate readable text
                textMutedColor = Color.parseColor("#065F46")        // Theme-tinted muted text
            )
            "PURPLE" -> PdfColorScheme(
                primaryColor = Color.parseColor("#581C87"),         // Deep Royal Purple
                secondaryColor = Color.parseColor("#7C3AED"),       // Vibrant Violet
                headerBgColor = Color.parseColor("#FAF5FF"),        // Soft light lavender tint
                tableHeaderColor = Color.parseColor("#6D28D9"),     // Solid Purple table header
                tableHeaderTextColor = Color.WHITE,                // Crisp white text on dark header
                alternatingRowColor = Color.parseColor("#F5F3FF"),  // Subtle alternating row tint
                summaryBgColor = Color.parseColor("#F5F3FF"),       // Light lavender
                payableBgColor = Color.parseColor("#DDD6FE"),       // Distinct soft violet container
                payableTextColor = Color.parseColor("#4C1D95"),     // High contrast deep violet text
                borderColor = Color.parseColor("#A78BFA"),          // Theme border
                lightBorderColor = Color.parseColor("#EDE9FE"),     // Light divider border
                textColor = Color.parseColor("#0F172A"),            // Deep slate readable text
                textMutedColor = Color.parseColor("#6D28D9")        // Theme-tinted muted text
            )
            "SLATE" -> PdfColorScheme(
                primaryColor = Color.parseColor("#0F172A"),         // Deep Charcoal Slate
                secondaryColor = Color.parseColor("#334155"),       // Medium Slate
                headerBgColor = Color.parseColor("#F8FAFC"),        // Clean cool white/slate tint
                tableHeaderColor = Color.parseColor("#1E293B"),     // Solid dark slate table header
                tableHeaderTextColor = Color.WHITE,                // Crisp white text on dark header
                alternatingRowColor = Color.parseColor("#F1F5F9"),  // Subtle alternating row tint
                summaryBgColor = Color.parseColor("#F1F5F9"),       // Cool slate tint
                payableBgColor = Color.parseColor("#CBD5E1"),       // Distinct slate container
                payableTextColor = Color.parseColor("#0F172A"),     // High contrast dark slate text
                borderColor = Color.parseColor("#64748B"),          // Theme border
                lightBorderColor = Color.parseColor("#E2E8F0"),     // Light divider border
                textColor = Color.parseColor("#0F172A"),            // Deep slate readable text
                textMutedColor = Color.parseColor("#475569")        // Slate muted text
            )
            "CRIMSON", "RED" -> PdfColorScheme(
                primaryColor = Color.parseColor("#991B1B"),         // Deep Elegant Red
                secondaryColor = Color.parseColor("#DC2626"),       // Vibrant Red
                headerBgColor = Color.parseColor("#FEF2F2"),        // Soft light red/rose tint
                tableHeaderColor = Color.parseColor("#FECACA"),     // Same soft red as payable background
                tableHeaderTextColor = Color.parseColor("#7F1D1D"), // Deep red high contrast text matching payable text
                alternatingRowColor = Color.parseColor("#FFF1F2"),  // Subtle alternating row tint
                summaryBgColor = Color.parseColor("#FEF2F2"),       // Light red tint
                payableBgColor = Color.parseColor("#FECACA"),       // Distinct soft red container
                payableTextColor = Color.parseColor("#7F1D1D"),     // High contrast deep red text
                borderColor = Color.parseColor("#F87171"),          // Theme border
                lightBorderColor = Color.parseColor("#FEE2E2"),     // Light divider border
                textColor = Color.parseColor("#0F172A"),            // Deep slate readable text
                textMutedColor = Color.parseColor("#991B1B")        // Theme-tinted muted red text
            )
            "AMBER" -> PdfColorScheme(
                primaryColor = Color.parseColor("#78350F"),         // Warm Amber/Bronze
                secondaryColor = Color.parseColor("#B45309"),       // Vibrant Amber
                headerBgColor = Color.parseColor("#FFFBEB"),        // Light warm ivory tint
                tableHeaderColor = Color.parseColor("#92400E"),     // Solid warm amber table header
                tableHeaderTextColor = Color.WHITE,                // Crisp white text on dark header
                alternatingRowColor = Color.parseColor("#FEF3C7"),  // Subtle alternating row tint
                summaryBgColor = Color.parseColor("#FFFBEB"),       // Light ivory tint
                payableBgColor = Color.parseColor("#FDE68A"),       // Distinct warm amber container
                payableTextColor = Color.parseColor("#78350F"),     // High contrast dark amber text
                borderColor = Color.parseColor("#F59E0B"),          // Theme border
                lightBorderColor = Color.parseColor("#FEF3C7"),     // Light divider border
                textColor = Color.parseColor("#0F172A"),            // Deep slate readable text
                textMutedColor = Color.parseColor("#92400E")        // Theme-tinted muted text
            )
            "NAVY" -> PdfColorScheme(
                primaryColor = Color.parseColor("#1E3A8A"),         // Deep Navy Blue
                secondaryColor = Color.parseColor("#2563EB"),       // Royal Blue
                headerBgColor = Color.parseColor("#EFF6FF"),        // Soft light sky/blue tint
                tableHeaderColor = Color.parseColor("#1D4ED8"),     // Solid Navy/Blue table header
                tableHeaderTextColor = Color.WHITE,                // Crisp white text on dark header
                alternatingRowColor = Color.parseColor("#F0F7FF"),  // Subtle alternating row tint
                summaryBgColor = Color.parseColor("#F8FAFC"),       // Clean light blue-gray
                payableBgColor = Color.parseColor("#BFDBFE"),       // Distinct soft royal blue container
                payableTextColor = Color.parseColor("#1E3A8A"),     // High contrast deep navy text
                borderColor = Color.parseColor("#60A5FA"),          // Theme border
                lightBorderColor = Color.parseColor("#DBEAFE"),     // Light divider border
                textColor = Color.parseColor("#0F172A"),            // Deep slate readable text
                textMutedColor = Color.parseColor("#1E40AF")        // Theme-tinted muted text
            )
            else -> PdfColorScheme( // SLATE (Default - خاکستری مدرن)
                primaryColor = Color.parseColor("#0F172A"),         // Deep Charcoal Slate
                secondaryColor = Color.parseColor("#334155"),       // Medium Slate
                headerBgColor = Color.parseColor("#F8FAFC"),        // Clean cool white/slate tint
                tableHeaderColor = Color.parseColor("#1E293B"),     // Solid dark slate table header
                tableHeaderTextColor = Color.WHITE,                // Crisp white text on dark header
                alternatingRowColor = Color.parseColor("#F1F5F9"),  // Subtle alternating row tint
                summaryBgColor = Color.parseColor("#F1F5F9"),       // Cool slate tint
                payableBgColor = Color.parseColor("#CBD5E1"),       // Distinct slate container
                payableTextColor = Color.parseColor("#0F172A"),     // High contrast dark slate text
                borderColor = Color.parseColor("#64748B"),          // Theme border
                lightBorderColor = Color.parseColor("#E2E8F0"),     // Light divider border
                textColor = Color.parseColor("#0F172A"),            // Deep slate readable text
                textMutedColor = Color.parseColor("#475569")        // Slate muted text
            )
        }

        // Maintain the user's selected theme colors for all invoice types, including Proforma (پیش‌فاکتور)
        return base
    }

    private fun loadLogoBitmap(context: Context, logoUriStr: String?): Bitmap? {
        if (logoUriStr.isNullOrBlank()) return null
        return try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }
            val file = File(logoUriStr)
            if (file.exists() && file.canRead()) {
                BitmapFactory.decodeFile(file.absolutePath, options)
            } else {
                val uri = Uri.parse(logoUriStr)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()
                bmp
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates a standard A4 Portrait PDF for the invoice.
     * Returns the File object pointing to the generated PDF.
     */
    fun generateInvoicePdf(
        context: Context,
        invoiceWithItems: InvoiceWithItems,
        settings: BusinessSettings
    ): File {
        val invoice = invoiceWithItems.invoice
        val items = invoiceWithItems.items
        val isProforma = invoice.invoiceType == InvoiceType.PROFORMA.name
        val colorTheme = getPdfColorScheme(settings.pdfTheme, isProforma)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Load Vazirmatn font for PDF rendering
        val regularTypeface = try {
            ResourcesCompat.getFont(context, R.font.vazirmatn) ?: Typeface.DEFAULT
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
        val boldTypeface = try {
            ResourcesCompat.getFont(context, R.font.vazirmatn_bold) ?: Typeface.create(regularTypeface, Typeface.BOLD)
        } catch (e: Exception) {
            Typeface.create(regularTypeface, Typeface.BOLD)
        }

        // Common Paints
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTheme.textColor
            typeface = regularTypeface
            textSize = 9.5f
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTheme.textColor
            typeface = boldTypeface
            textSize = 10.5f
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTheme.lightBorderColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val textPaintWrapped = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTheme.textColor
            textSize = 9f
            typeface = regularTypeface
        }

        // 1. Outer Frame Box
        val frameLeft = OUTER_MARGIN
        val frameTop = OUTER_MARGIN
        val frameRight = PAGE_WIDTH - OUTER_MARGIN
        val frameBottom = PAGE_HEIGHT - OUTER_MARGIN

        borderPaint.strokeWidth = 1.2f
        borderPaint.color = colorTheme.secondaryColor
        canvas.drawRoundRect(
            RectF(frameLeft, frameTop, frameRight, frameBottom),
            CORNER_RADIUS, CORNER_RADIUS, borderPaint
        )

        // 2. Inner Content Bounds with safe spacing to avoid overlapping the outer frame
        val contentLeft = frameLeft + INNER_PADDING
        val contentRight = frameRight - INNER_PADDING
        val contentWidth = contentRight - contentLeft
        var currentY = frameTop + INNER_PADDING

        // ==========================================
        // 1. HEADER SECTION
        // ==========================================
        val headerHeight = 72f
        val headerRect = RectF(contentLeft, currentY, contentRight, currentY + headerHeight)

        fillPaint.color = colorTheme.headerBgColor
        canvas.drawRoundRect(headerRect, CORNER_RADIUS, CORNER_RADIUS, fillPaint)
        borderPaint.color = colorTheme.borderColor
        canvas.drawRoundRect(headerRect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)

        // A. Draw Logo on Right Side (if available, preserving original aspect ratio and high resolution)
        val logoBitmap = loadLogoBitmap(context, settings.logoUri)
        if (logoBitmap != null && logoBitmap.width > 0 && logoBitmap.height > 0) {
            val maxLogoWidth = 68f
            val maxLogoHeight = headerHeight - 16f // 56f
            val ratio = minOf(maxLogoWidth / logoBitmap.width.toFloat(), maxLogoHeight / logoBitmap.height.toFloat())
            val targetW = logoBitmap.width * ratio
            val targetH = logoBitmap.height * ratio

            val drawX = contentRight - 12f - targetW
            val drawY = currentY + (headerHeight - targetH) / 2f
            val dstRect = RectF(drawX, drawY, drawX + targetW, drawY + targetH)

            val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                isDither = true
            }
            canvas.drawBitmap(logoBitmap, null, dstRect, imagePaint)
        }

        // B. Center: Store / Company Name (Top) & Invoice Type (Below)
        val storeName = if (invoice.sellerName.isNotBlank()) invoice.sellerName else settings.businessName
        boldPaint.textSize = 14.5f
        boldPaint.color = colorTheme.primaryColor
        boldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(storeName, contentLeft + contentWidth / 2, currentY + 28f, boldPaint)

        val titleText = if (isProforma) "پیش‌فاکتور فروش کالا و خدمات" else "فاکتور فروش کالا و خدمات"
        boldPaint.textSize = 11.5f
        boldPaint.color = colorTheme.secondaryColor
        canvas.drawText(titleText, contentLeft + contentWidth / 2, currentY + 50f, boldPaint)

        // C. Left: Meta closer to left margin (Values, Colons, Labels tightly grouped near left border)
        val colonX = contentLeft + 54f

        // Row 1: شماره
        boldPaint.textSize = 9.5f
        boldPaint.color = colorTheme.textColor
        boldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("شماره", colonX + 5f, currentY + 30f, boldPaint)

        boldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(":", colonX, currentY + 30f, boldPaint)

        textPaint.textSize = 9.5f
        textPaint.color = colorTheme.textColor
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(PersianNumberHelper.toPersianDigits(invoice.invoiceNumber), colonX - 5f, currentY + 30f, textPaint)

        // Row 2: تاریخ
        boldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("تاریخ", colonX + 5f, currentY + 50f, boldPaint)

        boldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(":", colonX, currentY + 50f, boldPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(PersianNumberHelper.toPersianDigits(invoice.issueDateShamsi), colonX - 5f, currentY + 50f, textPaint)

        currentY += headerHeight + 8f

        // ==========================================
        // 2. SELLER & BUYER DETAILS BOXES
        // ==========================================
        val infoBoxHeight = 54f
        val halfWidth = (contentWidth - 6f) / 2

        // Seller Box (Right Half in RTL)
        val sellerRect = RectF(contentLeft + halfWidth + 6f, currentY, contentRight, currentY + infoBoxHeight)
        fillPaint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(sellerRect, CORNER_RADIUS, CORNER_RADIUS, fillPaint)
        borderPaint.color = colorTheme.lightBorderColor
        canvas.drawRoundRect(sellerRect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)

        boldPaint.textSize = 9.5f
        boldPaint.color = colorTheme.primaryColor
        boldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("مشخصات فروشنده", sellerRect.right - 8f, currentY + 14f, boldPaint)

        textPaint.textSize = 8.5f
        textPaint.color = colorTheme.textColor
        textPaint.textAlign = Paint.Align.RIGHT
        val sellerNameStr = if (invoice.sellerName.isNotBlank()) invoice.sellerName else settings.businessName
        val sellerPhoneStr = if (invoice.sellerPhone.isNotBlank()) invoice.sellerPhone else settings.businessPhone
        val sellerAddrStr = if (invoice.sellerAddress.isNotBlank()) invoice.sellerAddress else settings.businessAddress

        canvas.drawText("نام/فروشگاه: $sellerNameStr", sellerRect.right - 8f, currentY + 27f, textPaint)
        canvas.drawText("تلفن: ${PersianNumberHelper.toPersianDigits(sellerPhoneStr)}", sellerRect.right - 8f, currentY + 39f, textPaint)
        val truncatedSellerAddr = if (sellerAddrStr.length > 42) sellerAddrStr.substring(0, 39) + "..." else sellerAddrStr
        canvas.drawText("نشانی: $truncatedSellerAddr", sellerRect.right - 8f, currentY + 49f, textPaint)

        // Buyer Box (Left Half)
        val buyerRect = RectF(contentLeft, currentY, contentLeft + halfWidth, currentY + infoBoxHeight)
        canvas.drawRoundRect(buyerRect, CORNER_RADIUS, CORNER_RADIUS, fillPaint)
        canvas.drawRoundRect(buyerRect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)

        canvas.drawText("مشخصات خریدار", buyerRect.right - 8f, currentY + 14f, boldPaint)
        canvas.drawText("نام خریدار: ${if (invoice.customerName.isNotBlank()) invoice.customerName else "خریدار متفرقه"}", buyerRect.right - 8f, currentY + 27f, textPaint)
        canvas.drawText("تلفن تماس: ${PersianNumberHelper.toPersianDigits(invoice.customerPhone)}", buyerRect.right - 8f, currentY + 39f, textPaint)
        val truncatedBuyerAddr = if (invoice.customerAddress.length > 42) invoice.customerAddress.substring(0, 39) + "..." else invoice.customerAddress
        canvas.drawText("نشانی: $truncatedBuyerAddr", buyerRect.right - 8f, currentY + 49f, textPaint)

        currentY += infoBoxHeight + 8f

        // ==========================================
        // 3. PRODUCTS & SERVICES TABLE
        // ==========================================
        val colRowWidth = 26f
        val colCodeWidth = 52f
        val colQtyWidth = 42f
        val colUnitPriceWidth = 88f
        val colTotalPriceWidth = 98f
        val colNameWidth = contentWidth - (colRowWidth + colCodeWidth + colQtyWidth + colUnitPriceWidth + colTotalPriceWidth)

        val colX_Right = contentRight
        val colX_Row = colX_Right - colRowWidth
        val colX_Name = colX_Row - colNameWidth
        val colX_Code = colX_Name - colCodeWidth
        val colX_Qty = colX_Code - colQtyWidth
        val colX_UnitPrice = colX_Qty - colUnitPriceWidth
        val colX_TotalPrice = contentLeft

        val tableHeaderHeight = 22f
        val rowHeight = 20f
        val maxRows = 14
        val displayItems = items.take(maxRows)
        val emptyRowsToAdd = if (displayItems.size < maxRows) maxOf(1, 6 - displayItems.size) else 0
        val totalTableRows = displayItems.size + emptyRowsToAdd
        val totalTableHeight = tableHeaderHeight + (totalTableRows * rowHeight)

        val tableRect = RectF(contentLeft, currentY, contentRight, currentY + totalTableHeight)

        // Clip entire table to CORNER_RADIUS rounded rectangle so all 4 corners are smooth
        val tablePath = Path().apply {
            addRoundRect(tableRect, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(tablePath)

        // Table Header Fill
        val tableHeaderRect = RectF(contentLeft, currentY, contentRight, currentY + tableHeaderHeight)
        fillPaint.color = colorTheme.tableHeaderColor
        canvas.drawRect(tableHeaderRect, fillPaint)

        boldPaint.textSize = 9f
        boldPaint.color = colorTheme.tableHeaderTextColor
        boldPaint.textAlign = Paint.Align.CENTER

        canvas.drawText("ردیف", (colX_Right + colX_Row) / 2, currentY + 14f, boldPaint)
        canvas.drawText("شرح کالا یا خدمات", (colX_Row + colX_Name) / 2, currentY + 14f, boldPaint)
        canvas.drawText("کد کالا", (colX_Name + colX_Code) / 2, currentY + 14f, boldPaint)
        canvas.drawText("تعداد", (colX_Code + colX_Qty) / 2, currentY + 14f, boldPaint)
        canvas.drawText("مبلغ واحد (${invoice.currency})", (colX_Qty + colX_UnitPrice) / 2, currentY + 14f, boldPaint)
        canvas.drawText("مبلغ کل (${invoice.currency})", (colX_UnitPrice + colX_TotalPrice) / 2, currentY + 14f, boldPaint)

        var rowY = currentY + tableHeaderHeight

        // Table Rows
        textPaint.textSize = 8.5f
        textPaint.color = colorTheme.textColor

        displayItems.forEachIndexed { index, item ->
            val rowRect = RectF(contentLeft, rowY, contentRight, rowY + rowHeight)
            if (index % 2 == 1) {
                fillPaint.color = colorTheme.alternatingRowColor
                canvas.drawRect(rowRect, fillPaint)
            } else {
                fillPaint.color = Color.WHITE
                canvas.drawRect(rowRect, fillPaint)
            }
            borderPaint.color = colorTheme.lightBorderColor
            canvas.drawRect(rowRect, borderPaint)

            // Vertical Dividers
            canvas.drawLine(colX_Row, rowY, colX_Row, rowY + rowHeight, borderPaint)
            canvas.drawLine(colX_Name, rowY, colX_Name, rowY + rowHeight, borderPaint)
            canvas.drawLine(colX_Code, rowY, colX_Code, rowY + rowHeight, borderPaint)
            canvas.drawLine(colX_Qty, rowY, colX_Qty, rowY + rowHeight, borderPaint)
            canvas.drawLine(colX_UnitPrice, rowY, colX_UnitPrice, rowY + rowHeight, borderPaint)

            // Row #
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(PersianNumberHelper.toPersianDigits((index + 1).toString()), (colX_Right + colX_Row) / 2, rowY + 13f, textPaint)

            // Item Name (Auto-detect text length and scale font / wrap so full text is ALWAYS shown)
            val fullItemName = item.itemName.trim()
            val maxCellWidth = colX_Row - colX_Name - 8f
            val itemTextPaint = Paint(textPaint).apply {
                textAlign = Paint.Align.CENTER
            }

            var singleLineSize = 8.5f
            itemTextPaint.textSize = singleLineSize
            while (singleLineSize > 5.5f && itemTextPaint.measureText(fullItemName) > maxCellWidth) {
                singleLineSize -= 0.5f
                itemTextPaint.textSize = singleLineSize
            }

            if (itemTextPaint.measureText(fullItemName) <= maxCellWidth) {
                canvas.drawText(fullItemName, (colX_Row + colX_Name) / 2, rowY + 13f, itemTextPaint)
            } else {
                val words = fullItemName.split(Regex("\\s+"))
                var twoLines: Pair<String, String>? = null

                var wrapFontSize = 6.5f
                while (wrapFontSize >= 4.0f && twoLines == null) {
                    itemTextPaint.textSize = wrapFontSize
                    if (words.size > 1) {
                        for (splitIdx in 1 until words.size) {
                            val part1 = words.subList(0, splitIdx).joinToString(" ")
                            val part2 = words.subList(splitIdx, words.size).joinToString(" ")
                            if (itemTextPaint.measureText(part1) <= maxCellWidth && itemTextPaint.measureText(part2) <= maxCellWidth) {
                                twoLines = Pair(part1, part2)
                                break
                            }
                        }
                    }
                    if (twoLines == null) {
                        wrapFontSize -= 0.5f
                    }
                }

                if (twoLines != null) {
                    itemTextPaint.textSize = wrapFontSize
                    canvas.drawText(twoLines.first, (colX_Row + colX_Name) / 2, rowY + 8.5f, itemTextPaint)
                    canvas.drawText(twoLines.second, (colX_Row + colX_Name) / 2, rowY + 16f, itemTextPaint)
                } else {
                    var fallbackSize = wrapFontSize.coerceAtLeast(3.5f)
                    itemTextPaint.textSize = fallbackSize
                    while (fallbackSize > 3.0f && itemTextPaint.measureText(fullItemName) > maxCellWidth) {
                        fallbackSize -= 0.3f
                        itemTextPaint.textSize = fallbackSize
                    }
                    canvas.drawText(fullItemName, (colX_Row + colX_Name) / 2, rowY + 13f, itemTextPaint)
                }
            }

            // Item Code (Centered)
            val codeStr = if (item.itemCode.isNotBlank()) PersianNumberHelper.toPersianDigits(item.itemCode) else "-"
            canvas.drawText(codeStr, (colX_Name + colX_Code) / 2, rowY + 13f, textPaint)

            // Qty (Centered)
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            canvas.drawText(PersianNumberHelper.toPersianDigits(qtyStr), (colX_Code + colX_Qty) / 2, rowY + 13f, textPaint)

            // Unit Price (Centered)
            canvas.drawText(PersianNumberHelper.formatPriceNumberOnly(item.unitPrice), (colX_Qty + colX_UnitPrice) / 2, rowY + 13f, textPaint)

            // Total Price (Centered)
            canvas.drawText(PersianNumberHelper.formatPriceNumberOnly(item.totalPrice), (colX_UnitPrice + colX_TotalPrice) / 2, rowY + 13f, textPaint)

            rowY += rowHeight
        }

        // Empty Rows (with closing dash row after last item)
        for (i in 0 until emptyRowsToAdd) {
            val rowIndex = displayItems.size + i
            val rowRect = RectF(contentLeft, rowY, contentRight, rowY + rowHeight)
            if (rowIndex % 2 == 1) {
                fillPaint.color = colorTheme.alternatingRowColor
                canvas.drawRect(rowRect, fillPaint)
            } else {
                fillPaint.color = Color.WHITE
                canvas.drawRect(rowRect, fillPaint)
            }
            borderPaint.color = colorTheme.lightBorderColor
            canvas.drawRect(rowRect, borderPaint)

            canvas.drawLine(colX_Row, rowY, colX_Row, rowY + rowHeight, borderPaint)
            canvas.drawLine(colX_Name, rowY, colX_Name, rowY + rowHeight, borderPaint)
            canvas.drawLine(colX_Code, rowY, colX_Code, rowY + rowHeight, borderPaint)
            canvas.drawLine(colX_Qty, rowY, colX_Qty, rowY + rowHeight, borderPaint)
            canvas.drawLine(colX_UnitPrice, rowY, colX_UnitPrice, rowY + rowHeight, borderPaint)

            // If this is the row immediately following the last item, draw anti-tampering closing dashes
            if (i == 0) {
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = colorTheme.textMutedColor
                canvas.drawText("—", (colX_Right + colX_Row) / 2, rowY + 13f, textPaint)
                canvas.drawText("— — — — — — — — — — — — —", (colX_Row + colX_Name) / 2, rowY + 13f, textPaint)
                canvas.drawText("—", (colX_Name + colX_Code) / 2, rowY + 13f, textPaint)
                canvas.drawText("—", (colX_Code + colX_Qty) / 2, rowY + 13f, textPaint)
                canvas.drawText("—", (colX_Qty + colX_UnitPrice) / 2, rowY + 13f, textPaint)
                canvas.drawText("—", (colX_UnitPrice + colX_TotalPrice) / 2, rowY + 13f, textPaint)
                textPaint.color = colorTheme.textColor
            }

            rowY += rowHeight
        }

        canvas.restore()

        // Draw unified rounded border around the entire table
        borderPaint.color = colorTheme.borderColor
        canvas.drawRoundRect(tableRect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)

        currentY += totalTableHeight + 8f

        // ==========================================
        // 4. SUMMARY & WORDS TABLE (Harmonious & Unified Rounded Card)
        // ==========================================
        val summaryRowHeight = 19f
        val payableRowHeight = 22f
        val summaryLeft = contentLeft
        val summaryRight = contentRight

        // Prepare StaticLayout for words (Left aligned like numbers on the left)
        val wordsText = PersianNumberHelper.numberToPersianWords(invoice.finalTotal, invoice.currency)
        textPaintWrapped.color = colorTheme.textColor
        textPaintWrapped.typeface = boldTypeface

        val wordsAvailableWidth = (contentWidth - 110f).toInt()
        val staticWordsLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(wordsText, 0, wordsText.length, textPaintWrapped, wordsAvailableWidth)
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
                .setLineSpacing(2f, 1f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(wordsText, textPaintWrapped, wordsAvailableWidth, Layout.Alignment.ALIGN_OPPOSITE, 1f, 2f, false)
        }
        val wordsRowHeight = maxOf(20f, staticWordsLayout.height + 10f)

        val totalSummaryHeight = (summaryRowHeight * 3) + payableRowHeight + wordsRowHeight
        val summaryRect = RectF(summaryLeft, currentY, summaryRight, currentY + totalSummaryHeight)

        // Clip entire Summary Table to CORNER_RADIUS rounded rectangle
        val summaryPath = Path().apply {
            addRoundRect(summaryRect, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(summaryPath)

        var sY = currentY

        // 1. Subtotal Row
        fillPaint.color = colorTheme.summaryBgColor
        canvas.drawRect(RectF(summaryLeft, sY, summaryRight, sY + summaryRowHeight), fillPaint)
        borderPaint.color = colorTheme.lightBorderColor
        canvas.drawRect(RectF(summaryLeft, sY, summaryRight, sY + summaryRowHeight), borderPaint)

        boldPaint.textSize = 8.5f
        boldPaint.color = colorTheme.textColor
        boldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("جمع کل اقلام:", summaryRight - 10f, sY + 13f, boldPaint)

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 9f
        canvas.drawText(PersianNumberHelper.formatPrice(invoice.subtotal, invoice.currency), summaryLeft + 10f, sY + 13f, textPaint)

        sY += summaryRowHeight

        // 2. Discount Row
        fillPaint.color = Color.WHITE
        canvas.drawRect(RectF(summaryLeft, sY, summaryRight, sY + summaryRowHeight), fillPaint)
        canvas.drawRect(RectF(summaryLeft, sY, summaryRight, sY + summaryRowHeight), borderPaint)

        val discountLabel = if (invoice.discountPercent > 0) "تخفیف (${invoice.discountPercent}%):" else "تخفیف:"
        canvas.drawText(discountLabel, summaryRight - 10f, sY + 13f, boldPaint)
        canvas.drawText(PersianNumberHelper.formatPrice(invoice.discountAmount, invoice.currency), summaryLeft + 10f, sY + 13f, textPaint)

        sY += summaryRowHeight

        // 3. VAT Row
        fillPaint.color = colorTheme.summaryBgColor
        canvas.drawRect(RectF(summaryLeft, sY, summaryRight, sY + summaryRowHeight), fillPaint)
        canvas.drawRect(RectF(summaryLeft, sY, summaryRight, sY + summaryRowHeight), borderPaint)

        val vatLabel = if (invoice.vatPercent > 0) "مالیات و عوارض ارزش افزوده (${invoice.vatPercent}%):" else "مالیات و عوارض ارزش افزوده:"
        canvas.drawText(vatLabel, summaryRight - 10f, sY + 13f, boldPaint)
        canvas.drawText(PersianNumberHelper.formatPrice(invoice.vatAmount, invoice.currency), summaryLeft + 10f, sY + 13f, textPaint)

        sY += summaryRowHeight

        // 4. Final Payable Total Row (Harmonious alternating white background with bold primary text)
        fillPaint.color = Color.WHITE
        canvas.drawRect(RectF(summaryLeft, sY, summaryRight, sY + payableRowHeight), fillPaint)
        borderPaint.color = colorTheme.lightBorderColor
        canvas.drawRect(RectF(summaryLeft, sY, summaryRight, sY + payableRowHeight), borderPaint)

        boldPaint.textSize = 9.5f
        boldPaint.color = colorTheme.primaryColor
        boldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("مبلغ قابل پرداخت:", summaryRight - 10f, sY + 15f, boldPaint)

        boldPaint.textAlign = Paint.Align.LEFT
        boldPaint.textSize = 10f
        canvas.drawText(PersianNumberHelper.formatPrice(invoice.finalTotal, invoice.currency), summaryLeft + 10f, sY + 15f, boldPaint)

        sY += payableRowHeight

        // 5. Amount in Persian Words Row (Harmonious alternating light-tint background)
        fillPaint.color = colorTheme.summaryBgColor
        canvas.drawRect(RectF(summaryLeft, sY, summaryRight, sY + wordsRowHeight), fillPaint)
        borderPaint.color = colorTheme.lightBorderColor
        canvas.drawRect(RectF(summaryLeft, sY, summaryRight, sY + wordsRowHeight), borderPaint)

        boldPaint.textSize = 8.5f
        boldPaint.color = colorTheme.textColor
        boldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("مبلغ به حروف:", summaryRight - 10f, sY + 14f, boldPaint)

        // Draw words value aligned to the LEFT side of the table
        canvas.save()
        canvas.translate(summaryLeft + 10f, sY + 4f)
        staticWordsLayout.draw(canvas)
        canvas.restore()

        canvas.restore()

        // Outer rounded border of the unified summary card
        borderPaint.color = colorTheme.borderColor
        canvas.drawRoundRect(summaryRect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)

        currentY += totalSummaryHeight + 8f

        // ==========================================
        // 5. BANK SETTLEMENT ACCOUNT DETAILS SECTION (Independent Card)
        // ==========================================
        val hasBankInfo = invoice.bankName.isNotBlank() || invoice.bankCardNumber.isNotBlank() ||
                invoice.bankIban.isNotBlank() || invoice.bankAccountNumber.isNotBlank()

        if (hasBankInfo) {
            val bName = invoice.bankName
            val bBranch = invoice.bankBranch
            val bHolder = invoice.bankAccountHolder
            val bCard = invoice.bankCardNumber
            val bIban = invoice.bankIban
            val bAccount = invoice.bankAccountNumber
            val bNotes = invoice.bankDepositNotes

            val bankBoxHeight = if (bNotes.isNotBlank()) 50f else 40f
            val bankRect = RectF(contentLeft, currentY, contentRight, currentY + bankBoxHeight)

            fillPaint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(bankRect, CORNER_RADIUS, CORNER_RADIUS, fillPaint)
            borderPaint.color = colorTheme.lightBorderColor
            canvas.drawRoundRect(bankRect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)

            // Header line / Title
            boldPaint.textSize = 8.5f
            boldPaint.color = colorTheme.secondaryColor
            boldPaint.textAlign = Paint.Align.RIGHT
            val bankTitle = "اطلاعات حساب بانکی جهت واریز و تسویه حساب"
            canvas.drawText(bankTitle, contentRight - 10f, currentY + 13f, boldPaint)

            // Details line 1: Bank Name + Holder
            textPaint.textSize = 8f
            textPaint.color = colorTheme.textColor
            textPaint.textAlign = Paint.Align.RIGHT

            val bankInfoStr = buildString {
                if (bName.isNotBlank()) {
                    append("بانک: $bName")
                    if (bBranch.isNotBlank()) append(" (شعبه $bBranch)")
                }
                if (bHolder.isNotBlank()) {
                    if (isNotEmpty()) append("   |   ")
                    append("صاحب حساب: $bHolder")
                }
            }
            canvas.drawText(PersianNumberHelper.toPersianDigits(bankInfoStr), contentRight - 10f, currentY + 24f, textPaint)

            // Details line 2: Card & IBAN & Account (card formatted in 4-digit blocks with hyphens only, no spaces)
            val cardFormatted = PersianNumberHelper.formatCardNumber(bCard)

            val numInfoStr = buildString {
                if (bCard.isNotBlank()) append("شماره کارت: \u200E$cardFormatted\u200E")
                if (bIban.isNotBlank()) {
                    if (isNotEmpty()) append("   |   ")
                    append("شبا: \u200E$bIban\u200E")
                }
                if (bAccount.isNotBlank()) {
                    if (isNotEmpty()) append("   |   ")
                    append("شماره حساب: \u200E$bAccount\u200E")
                }
            }
            canvas.drawText(PersianNumberHelper.toPersianDigits(numInfoStr), contentRight - 10f, currentY + 34f, textPaint)

            if (bNotes.isNotBlank() && bankBoxHeight >= 50f) {
                textPaint.color = colorTheme.textMutedColor
                canvas.drawText(PersianNumberHelper.toPersianDigits("توضیحات واریز: $bNotes"), contentRight - 10f, currentY + 44f, textPaint)
            }

            currentY += bankBoxHeight + 8f
        }

        // ==========================================
        // 6. NOTES & TERMS SECTION (Word Wrapped, Uniform Rounded Box)
        // ==========================================
        val effectiveNotes = if (invoice.notes.isNotBlank()) invoice.notes else settings.defaultFooterNote
        textPaintWrapped.color = colorTheme.textMutedColor
        textPaintWrapped.typeface = regularTypeface

        val notesAvailableWidth = (contentWidth - 24f).toInt()
        val staticNotesLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(effectiveNotes, 0, effectiveNotes.length, textPaintWrapped, notesAvailableWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(effectiveNotes, textPaintWrapped, notesAvailableWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 2f, false)
        }

        val notesHeight = maxOf(36f, staticNotesLayout.height + 22f)
        val notesRect = RectF(contentLeft, currentY, contentRight, currentY + notesHeight)
        fillPaint.color = Color.parseColor("#FAFAFA")
        canvas.drawRoundRect(notesRect, CORNER_RADIUS, CORNER_RADIUS, fillPaint)
        borderPaint.color = colorTheme.lightBorderColor
        canvas.drawRoundRect(notesRect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)

        boldPaint.textSize = 8.5f
        boldPaint.color = colorTheme.textColor
        boldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("توضیحات و شرایط:", contentRight - 10f, currentY + 13f, boldPaint)

        canvas.save()
        canvas.translate(contentRight - 10f - staticNotesLayout.width, currentY + 18f)
        staticNotesLayout.draw(canvas)
        canvas.restore()

        currentY += notesHeight + 8f

        // ==========================================
        // 6. SIGNATURES & STAMPS SECTION
        // ==========================================
        val sigBoxHeight = 56f
        val sigBoxWidth = (contentWidth - 10f) / 2

        // Seller Signature Box
        val sellerSigRect = RectF(contentLeft + sigBoxWidth + 10f, currentY, contentRight, currentY + sigBoxHeight)
        fillPaint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(sellerSigRect, CORNER_RADIUS, CORNER_RADIUS, fillPaint)
        borderPaint.color = colorTheme.lightBorderColor
        canvas.drawRoundRect(sellerSigRect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)

        boldPaint.textAlign = Paint.Align.CENTER
        boldPaint.color = colorTheme.textMutedColor
        boldPaint.textSize = 8.5f
        canvas.drawText("مهر و امضاء فروشنده", (sellerSigRect.left + sellerSigRect.right) / 2, currentY + 14f, boldPaint)

        // Draw seller stamp/signature image if provided in settings (preserving full high-resolution)
        val signatureBitmap = loadLogoBitmap(context, settings.signatureUri)
        if (signatureBitmap != null && signatureBitmap.width > 0 && signatureBitmap.height > 0) {
            val maxSigWidth = sellerSigRect.width() - 16f
            val maxSigHeight = sigBoxHeight - 20f
            if (maxSigWidth > 0 && maxSigHeight > 0) {
                val ratio = minOf(maxSigWidth / signatureBitmap.width.toFloat(), maxSigHeight / signatureBitmap.height.toFloat())
                val targetW = signatureBitmap.width * ratio
                val targetH = signatureBitmap.height * ratio

                val drawX = sellerSigRect.left + (sellerSigRect.width() - targetW) / 2f
                val drawY = currentY + 16f + (maxSigHeight - targetH) / 2f
                val dstRect = RectF(drawX, drawY, drawX + targetW, drawY + targetH)

                val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    isDither = true
                }
                canvas.drawBitmap(signatureBitmap, null, dstRect, imagePaint)
            }
        }

        // Buyer Signature Box
        val buyerSigRect = RectF(contentLeft, currentY, contentLeft + sigBoxWidth, currentY + sigBoxHeight)
        canvas.drawRoundRect(buyerSigRect, CORNER_RADIUS, CORNER_RADIUS, fillPaint)
        canvas.drawRoundRect(buyerSigRect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)
        canvas.drawText("مهر و امضاء خریدار", (buyerSigRect.left + buyerSigRect.right) / 2, currentY + 14f, boldPaint)

        // Finish PDF Page
        pdfDocument.finishPage(page)

        // Save PDF to cache dir
        val outputDir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val fileName = "Invoice_${invoice.invoiceNumber.replace("/", "_")}_${System.currentTimeMillis()}.pdf"
        val outputFile = File(outputDir, fileName)

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    /**
     * Shares the generated PDF file using standard Android Intent chooser.
     */
    fun shareInvoicePdf(context: Context, pdfFile: File, invoiceTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, invoiceTitle)
            putExtra(Intent.EXTRA_TEXT, "فایل PDF $invoiceTitle")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "اشتراک‌گذاری فاکتور PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Opens the generated PDF file in an external PDF viewer.
     */
    fun viewInvoicePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            val chooser = Intent.createChooser(intent, "نمایش فاکتور")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback to share
            shareInvoicePdf(context, pdfFile, "فاکتور")
        }
    }
}
