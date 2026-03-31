package com.rishi.fintrack.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.*

fun generateProfessionalReport(context: Context, transactions: List<com.rishi.fintrack.data.Transaction>) {
    try {
        val fileName = "FinTrack_Audit_Report_${System.currentTimeMillis()}.pdf"
        val outputStream: OutputStream?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/FinTrack")
            }
            val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val folder = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "FinTrack")
            if (!folder.exists()) folder.mkdirs()
            outputStream = java.io.FileOutputStream(java.io.File(folder, fileName))
        }

        if (outputStream == null) return

        val writer = PdfWriter(outputStream)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        // --- THEME MATCHING COLORS ---
        val primaryIndigo = DeviceRgb(99, 102, 241) // #6366F1
        val secondaryCyan = DeviceRgb(34, 211, 238)  // #22D3EE
        val textGray = DeviceRgb(148, 163, 184)
        val white = DeviceRgb.WHITE

        // --- 1. BRANDED HEADER (Logo + Stylish Name) ---
        // FIX: Space ko 15f se 25f kiya taaki lamba logo dabbe nahi
        // --- 1. BRANDED HEADER (Logo + Stylish Name) ---
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f))).useAllAvailableWidth()
        try {
            // FIX: app_logo ki jagah ui_logo kar diya taaki PDF me tight aur HD logo aaye
            val bitmap = BitmapFactory.decodeResource(context.resources, com.rishi.fintrack.R.drawable.ui_logo)
            val stream = ByteArrayOutputStream()
            // Quality 100 rakhi hai aur direct image pass ki hai taaki fate nahi
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)

            // iText automatically aspect ratio maintain karega, humne sirf width 140f de di
            val logoImage = Image(ImageDataFactory.create(stream.toByteArray())).setWidth(140f)
            headerTable.addCell(Cell().add(logoImage).setBorder(Border.NO_BORDER))
        } catch (e: Exception) {
            headerTable.addCell(Cell().add(Paragraph("Logo")).setBorder(Border.NO_BORDER))
        }

        val brandInfo = Paragraph()
            .add(Text("FinTrack Pro\n").setFontSize(26f).setBold().setFontColor(primaryIndigo))
            .add(Text("Strategic Fiscal Intelligence & Wealth Audit Report").setFontSize(9f).setFontColor(textGray))
        headerTable.addCell(Cell().add(brandInfo).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE))
        document.add(headerTable)
        document.add(LineSeparator(SolidLine(1f)).setMarginTop(10f).setOpacity(0.3f))

        // --- 2. FISCAL EXECUTIVE SUMMARY ---
        val totalInc = transactions.filter { it.type == "Income" }.sumOf { it.amount.toDouble() }
        val totalExp = transactions.filter { it.type == "Expense" }.sumOf { it.amount.toDouble() }

        document.add(Paragraph("\n1.0 Fiscal Executive Summary").setBold().setFontSize(14f).setFontColor(primaryIndigo))
        val summaryTable = Table(3).useAllAvailableWidth().setMarginTop(10f)

        fun createCell(label: String, value: String, color: DeviceRgb): Cell {
            return Cell().add(Paragraph(label).setFontSize(8f).setFontColor(textGray))
                .add(Paragraph(value).setBold().setFontSize(14f).setFontColor(color))
                .setPadding(10f).setBorder(SolidBorder(DeviceRgb(230, 230, 230), 0.5f))
        }

        summaryTable.addCell(createCell("TOTAL INFLOW", "₹${totalInc.toInt()}", DeviceRgb(50, 215, 75)))
        summaryTable.addCell(createCell("TOTAL OUTFLOW", "₹${totalExp.toInt()}", DeviceRgb(255, 69, 58)))
        summaryTable.addCell(createCell("NET SAVINGS", "₹${(totalInc - totalExp).toInt()}", primaryIndigo))
        document.add(summaryTable)

        // --- 3. EXPENDITURE POLE CHART (Multi-color) ---
        document.add(Paragraph("\n2.0 Expenditure Distribution (Pole Chart)").setBold().setFontSize(12f).setMarginTop(15f))
        val expensesByCategory = transactions.filter { it.type == "Expense" }.groupBy { it.category }
        val maxExp = expensesByCategory.values.map { it.sumOf { t -> t.amount.toDouble() } }.maxOrNull() ?: 1.0

        val chartBox = Table(1).useAllAvailableWidth().setBorder(SolidBorder(primaryIndigo, 0.5f)).setPadding(15f)
        expensesByCategory.forEach { (cat, list) ->
            val amt = list.sumOf { it.amount.toDouble() }
            val width = (amt / maxExp * 100).toFloat()

            val barRow = Table(UnitValue.createPercentArray(floatArrayOf(20f, 80f))).useAllAvailableWidth()
            barRow.addCell(Cell().add(Paragraph(cat).setFontSize(8f).setBold()).setBorder(Border.NO_BORDER))

            val barWrapper = Table(UnitValue.createPercentArray(floatArrayOf(width, 100f - width))).useAllAvailableWidth()
            barWrapper.addCell(Cell().setBackgroundColor(primaryIndigo).setHeight(8f).setBorder(Border.NO_BORDER))
            barWrapper.addCell(Cell().setBorder(Border.NO_BORDER))

            barRow.addCell(Cell().add(barWrapper).setBorder(Border.NO_BORDER))
            chartBox.addCell(Cell().add(barRow).setBorder(Border.NO_BORDER).setPaddingBottom(5f))
        }
        document.add(chartBox)

        // --- 4. STRATEGIC AI INSIGHTS ---
        document.add(Paragraph("\n3.0 Strategic AI Audit Observations").setBold().setFontSize(14f).setFontColor(primaryIndigo))
        expensesByCategory.forEach { (cat, list) ->
            val amt = list.sumOf { it.amount.toDouble() }
            val pct = if (totalExp > 0) (amt / totalExp * 100).toInt() else 0
            document.add(Paragraph("Audit Focus: $cat Management").setBold().setFontSize(10f).setFontColor(primaryIndigo))
            document.add(Paragraph("• Fiscal Burn Rate: Your spending in $cat is $pct% of outflow. Professional modeling suggests reducing this by 0.5% weekly could yield ₹${(amt*0.1).toInt()} in liquidity.")
                .setFontSize(9f).setPaddingLeft(15f).setItalic())
        }

        // --- 5. TRANSACTION LEDGER ---
        document.add(AreaBreak())
        document.add(Paragraph("4.0 Verified Transaction Ledger").setBold().setFontSize(14f).setFontColor(primaryIndigo))
        val ledger = Table(UnitValue.createPercentArray(floatArrayOf(40f, 20f, 20f, 20f))).useAllAvailableWidth().setMarginTop(10f)
        arrayOf("Title", "Amount", "Type", "Category").forEach {
            ledger.addHeaderCell(Cell().add(Paragraph(it).setBold().setFontColor(white)).setBackgroundColor(primaryIndigo))
        }
        transactions.reversed().forEach { t ->
            ledger.addCell(Paragraph(t.title).setFontSize(9f))
            ledger.addCell(Paragraph("₹${t.amount.toInt()}").setFontSize(9f).setBold())
            ledger.addCell(Paragraph(t.type).setFontSize(9f))
            ledger.addCell(Paragraph(t.category).setFontSize(9f))
        }
        document.add(ledger)
        document.close()
        Toast.makeText(context, "VVIP Audit Report Generated!", Toast.LENGTH_LONG).show()
    } catch (e: Exception) { Toast.makeText(context, "Export Error: ${e.message}", Toast.LENGTH_SHORT).show() }
}