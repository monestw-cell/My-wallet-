package com.example.core.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfExporter {
    fun exportToPdf(context: Context, filename: String, title: String, contentLines: List<String>) {
        try {
            val pdfDocument = PdfDocument()
            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create() // A4 Size: 595 x 842
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint()
            val textPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }

            val titlePaint = TextPaint().apply {
                color = Color.rgb(0, 106, 106) // TealPrimary
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            var y = 50f
            
            // Draw clean Header
            canvas.drawText(title, 50f, y, titlePaint)
            y += 30f
            paint.color = Color.rgb(0, 106, 106)
            canvas.drawRect(50f, y, 545f, y + 2f, paint) // divider bar
            y += 30f

            contentLines.forEach { line ->
                if (y > 800f) {
                    pdfDocument.finishPage(page)
                    pageNum++
                    val nextInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                    page = pdfDocument.startPage(nextInfo)
                    canvas = page.canvas
                    y = 50f
                }
                // Draw line text
                canvas.drawText(line, 50f, y, textPaint)
                y += 24f
            }

            pdfDocument.finishPage(page)

            // Save the document to cache directory
            val file = File(context.cacheDir, "$filename.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()

            // Share the file
            val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "تصدير ومشاركة مستند PDF"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل تصدير ملف PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
