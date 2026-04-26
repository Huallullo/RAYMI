package com.raymi.app.data.remote

import android.content.Context
import android.os.Environment
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para generar PDFs de detalles de alquiler
 */
@Singleton
class PdfService @Inject constructor(
    private val context: Context
) {

    /**
     * Genera un PDF con el detalle del alquiler
     */
    suspend fun generarPdfAlquiler(alquiler: Alquiler): Resource<File> {
        return withContext(Dispatchers.IO) {
            try {
                // Crear directorio si no existe
                val pdfDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "RAYMI_PDFs")
                if (!pdfDir.exists()) {
                    pdfDir.mkdirs()
                }

                // Nombre del archivo
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "alquiler_${alquiler.id}_$timestamp.pdf"
                val pdfFile = File(pdfDir, fileName)

                // Crear PDF
                val writer = PdfWriter(pdfFile)
                val pdfDoc = PdfDocument(writer)
                val document = Document(pdfDoc)

                // Configurar márgenes profesionales
                document.setMargins(36f, 36f, 36f, 36f) // Izquierda, derecha, arriba, abajo

                // LOGO (si existe en drawable)
                try {
                    val resId = context.resources.getIdentifier("ic_raymi_logo", "drawable", context.packageName)
                    if (resId != 0) {
                        val bmp = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
                        val stream = java.io.ByteArrayOutputStream()
                        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                        val imgData = stream.toByteArray()
                        val img = com.itextpdf.io.image.ImageDataFactory.create(imgData)
                        val logo = com.itextpdf.layout.element.Image(img)
                            .setWidth(80f)
                            .setHeight(80f)
                            .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
                            .setMarginBottom(10f)
                        document.add(logo)
                    }
                } catch (_: Exception) {}

                // Título
                val title = Paragraph("DETALLE DEL ALQUILER - RAYMI")
                    .setFontSize(22f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.BLUE)
                    .setMarginBottom(18f)
                document.add(title)

                // Información del alquiler
                val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(35f, 65f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setMarginBottom(18f)

                // Cliente
                infoTable.addCell(celdaLabel("Cliente:"))
                infoTable.addCell(celdaValor(alquiler.clienteNombre))

                // Vestuario
                infoTable.addCell(celdaLabel("Vestuario:"))
                infoTable.addCell(celdaValor("${alquiler.vestuarioNombre} (Código: ${alquiler.vestuarioCodigo})"))

                // Cantidad
                infoTable.addCell(celdaLabel("Cantidad:"))
                infoTable.addCell(celdaValor("${alquiler.cantidad} unidad(es)"))

                // Fechas
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                infoTable.addCell(celdaLabel("Fecha de Inicio:"))
                infoTable.addCell(celdaValor(dateFormat.format(alquiler.fechaInicio.toDate())))

                infoTable.addCell(celdaLabel("Fecha de Devolución:"))
                infoTable.addCell(celdaValor(dateFormat.format(alquiler.fechaFinPrevista.toDate())))

                // Estado
                infoTable.addCell(celdaLabel("Estado:"))
                infoTable.addCell(celdaValor(alquiler.estado.name))

                document.add(infoTable)

                // Información de pago
                val paymentTitle = Paragraph("INFORMACIÓN DE PAGO")
                    .setFontSize(15f)
                    .setBold()
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setMarginTop(12f)
                    .setMarginBottom(8f)
                document.add(paymentTitle)

                val paymentTable = Table(UnitValue.createPercentArray(floatArrayOf(35f, 65f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setMarginBottom(18f)

                paymentTable.addCell(celdaLabel("Precio Unitario:"))
                paymentTable.addCell(celdaValor("S/. ${String.format("%.2f", alquiler.precioUnitario)}"))

                paymentTable.addCell(celdaLabel("Precio Total:"))
                paymentTable.addCell(celdaValor("S/. ${String.format("%.2f", alquiler.precioTotal)}"))

                paymentTable.addCell(celdaLabel("Adelanto:"))
                paymentTable.addCell(celdaValor("S/. ${String.format("%.2f", alquiler.adelanto)}"))

                paymentTable.addCell(celdaLabel("Saldo:"))
                paymentTable.addCell(celdaValor("S/. ${String.format("%.2f", alquiler.saldo)}"))

                document.add(paymentTable)

                // Observaciones
                if (alquiler.observaciones.isNotBlank()) {
                    val obsTitle = Paragraph("OBSERVACIONES")
                        .setFontSize(15f)
                        .setBold()
                        .setFontColor(ColorConstants.DARK_GRAY)
                        .setMarginTop(12f)
                        .setMarginBottom(8f)
                    document.add(obsTitle)

                    val obsParagraph = Paragraph(alquiler.observaciones)
                        .setMarginBottom(18f)
                    document.add(obsParagraph)
                }

                // Pie de página profesional
                val footer = Paragraph(
                    "Generado por RAYMI - Sistema de Alquiler de Vestuarios\n" +
                    "Contacto: contacto@raymi.com | Lima, Perú\n" +
                    "Fecha: ${dateFormat.format(Date())}"
                )
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginTop(30f)
                document.add(footer)

                document.close()

                Resource.Success(pdfFile)

            } catch (e: Exception) {
                Resource.Error("Error al generar PDF: ${e.message}")
            }
        }
    }

    // Helpers para formato de celdas
    private fun celdaLabel(text: String) = com.itextpdf.layout.element.Cell().add(Paragraph(text).setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY)
    private fun celdaValor(text: String) = com.itextpdf.layout.element.Cell().add(Paragraph(text))
}
