package com.exp.carconnect.report.pdf

import android.content.Context
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.view.View
import com.exp.carconnect.app.state.ReportData
import io.reactivex.Single
import java.io.File
import java.io.FileOutputStream
import java.util.*


class ReportPDFGenerator(private val context: Context) {


    fun generateReportPDF(report: ReportData, view: View): Single<String> {
        return Single.fromCallable {
            val file = File(context.getExternalFilesDir(null), (report.vin + "_ ${Date()}.pdf").replace(" ", ""))
            val outputStream = FileOutputStream(file)
            outputStream.use { outputStream ->
                val document = PdfDocument()
                val rect = Rect()
                view.getDrawingRect(rect)
                val pageInfo = PageInfo.Builder(rect.width(), rect.height(), 1).create()
                val page = document.startPage(pageInfo)
                view.draw(page.canvas)
                document.finishPage(page)
                document.writeTo(outputStream)
            }
            file.absolutePath
        }

    }
}